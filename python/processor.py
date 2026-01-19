import json
import sys
import cv2
import mediapipe as mp
import numpy as np
import torch
from collections import deque
import sklearn.preprocessing._label
import torch.nn.functional as F
import time
import threading
import os
from model import GestureLSTM

torch.serialization.add_safe_globals([sklearn.preprocessing._label.LabelEncoder])

# Определяем путь к модели относительно скрипта
script_dir = os.path.dirname(os.path.abspath(__file__))
model_path = os.path.join(script_dir, 'gesture_lstm.pth')

# Загрузка модели
checkpoint = torch.load(model_path, weights_only=False)
model = GestureLSTM(
    input_size=checkpoint['input_size'],
    num_classes=checkpoint['num_classes']
)
model.load_state_dict(checkpoint['model_state_dict'])
model.eval()
label_encoder = checkpoint['label_encoder']
SEQ_LENGTH = checkpoint['seq_length']
FEATURE_DIM = checkpoint['input_size']
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
model.to(device)

# Настройки улучшений
USE_RELATIVE_COORDS = True
USE_VELOCITY = True
USE_PALM_CENTER = True

# Настройки фильтрации
CONFIDENCE_THRESHOLD = 0.97
IGNORE_IDLE = True
DETECTION_COOLDOWN = 0.7

# НОВОЕ: Настройки фильтра стабильности для отсеивания вспомогательных движений
STABILITY_REQUIRED = 2  # Жест должен быть распознан N раз подряд
STABILITY_WINDOW = 2  # Проверяем последние N предсказаний
MIN_STABILITY_CONFIDENCE = 0.98  # Минимальная уверенность для зачета в стабильности


def extract_enhanced_features(landmarks, prev_coords=None):
    """Извлекает улучшенные признаки"""
    features = []
    wrist = landmarks[0]
    
    relative_coords = []
    for lm in landmarks:
        rel_x = lm.x - wrist.x
        rel_y = lm.y - wrist.y
        relative_coords.extend([rel_x, rel_y])
    
    features.extend(relative_coords)
    
    if USE_VELOCITY:
        if prev_coords is not None and len(prev_coords) >= 42:
            velocity = np.array(relative_coords) - np.array(prev_coords[:42])
            features.extend(velocity.tolist())
        else:
            features.extend([0.0] * 42)
    
    if USE_PALM_CENTER:
        palm_center_x = (landmarks[5].x + landmarks[9].x + landmarks[13].x + landmarks[17].x) / 4
        palm_center_y = (landmarks[5].y + landmarks[9].y + landmarks[13].y + landmarks[17].y) / 4
        
        palm_relative_coords = []
        for lm in landmarks:
            rel_x_palm = lm.x - palm_center_x
            rel_y_palm = lm.y - palm_center_y
            palm_relative_coords.extend([rel_x_palm, rel_y_palm])
        
        features.extend(palm_relative_coords)
    
    return np.array(features, dtype=np.float32), relative_coords

# MediaPipe - используем старый API (требует mediapipe < 0.10.22)
# Для 0.10.31 нужна модель .task файл - откати версию до 0.10.21
mp_hands = mp.solutions.hands
hands = mp_hands.Hands(static_image_mode=False, max_num_hands=1, min_detection_confidence=0.5)

# Буфер кадров
buffer = deque(maxlen=SEQ_LENGTH)
prev_frame_coords = None

# НОВОЕ: История предсказаний для проверки стабильности
prediction_history = deque(maxlen=STABILITY_WINDOW)  # Хранит последние N предсказаний

# Флаг для контроля детекции
detection_allowed = True
cooldown_timer = None
last_gesture = ""
last_confidence = 0.0

def reset_detection_allowed():
    """Сбрасывает флаг разрешения детекции через заданное время"""
    global detection_allowed
    time.sleep(DETECTION_COOLDOWN)
    detection_allowed = True

def check_gesture_stability(predicted_gesture, confidence_value):
    """
    НОВОЕ: Проверяет стабильность жеста - должен быть распознан несколько раз подряд
    
    Args:
        predicted_gesture: Предсказанный жест
        confidence_value: Уверенность модели
        
    Returns:
        bool: True если жест стабилен, False если это вспомогательное движение
    """
    # Добавляем текущее предсказание в историю
    prediction_history.append({
        'gesture': predicted_gesture,
        'confidence': confidence_value,
        'time': time.time()
    })
    
    # Если истории недостаточно, считаем жест нестабильным
    if len(prediction_history) < STABILITY_REQUIRED:
        return False
    
    # Проверяем последние N предсказаний
    recent_predictions = list(prediction_history)[-STABILITY_WINDOW:]
    
    # Считаем, сколько раз подряд был распознан этот жест
    consecutive_count = 0
    for pred in reversed(recent_predictions):
        if (pred['gesture'] == predicted_gesture and 
            pred['confidence'] >= MIN_STABILITY_CONFIDENCE):
            consecutive_count += 1
        else:
            break  # Прерываем, если жест изменился
    
    # Жест считается стабильным, если он распознан N раз подряд
    is_stable = consecutive_count >= STABILITY_REQUIRED
    
    return is_stable

cap = cv2.VideoCapture(0)

print("Запущено распознавание жестов")
print("Нажмите 'q' для выхода")

while cap.isOpened():
    ret, frame = cap.read()
    if not ret:
        continue

    frame = cv2.flip(frame, 1)
    rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    results = hands.process(rgb)

    # Обновляем буфер
    hand_detected = results.multi_hand_landmarks is not None
    
    if hand_detected:
        landmarks = results.multi_hand_landmarks[0].landmark
        coords, relative_coords = extract_enhanced_features(landmarks, prev_frame_coords)
        prev_frame_coords = relative_coords
        buffer.append(coords)
    else:
        if prev_frame_coords is not None:
            if USE_VELOCITY:
                zero_velocity = np.zeros(42, dtype=np.float32)
                if USE_PALM_CENTER:
                    coords = np.concatenate([
                        prev_frame_coords,
                        zero_velocity,
                        np.zeros(42, dtype=np.float32)
                    ]).astype(np.float32)
                else:
                    coords = np.concatenate([prev_frame_coords, zero_velocity]).astype(np.float32)
            else:
                coords = np.array(prev_frame_coords, dtype=np.float32)
            buffer.append(coords)
        else:
            buffer.append(np.zeros(FEATURE_DIM, dtype=np.float32))

    # Делаем предсказание только если разрешено и буфер полон
    if len(buffer) == SEQ_LENGTH and detection_allowed:
        # Предсказание
        seq = torch.tensor(np.array(buffer, dtype=np.float32), dtype=torch.float32).unsqueeze(0).to(device)
        with torch.no_grad():
            lengths = torch.tensor([SEQ_LENGTH], dtype=torch.long).to(device)
            output = model(seq, lengths)
            probabilities = F.softmax(output, dim=1)
            confidence, pred = torch.max(probabilities, dim=1)
            confidence_value = confidence.item()
            pred_class = pred.item()
            predicted_gesture = label_encoder.inverse_transform([pred_class])[0]
            
            # Базовые проверки
            should_show = True
            
            if confidence_value < CONFIDENCE_THRESHOLD:
                should_show = False
            
            if not hand_detected:
                should_show = False
            
            if IGNORE_IDLE and predicted_gesture.lower() == "idle":
                should_show = False
            
            # НОВОЕ: Проверка стабильности жеста
            if should_show:
                is_stable = check_gesture_stability(predicted_gesture, confidence_value)
                
                if is_stable:
                    # Жест стабилен - выводим результат в поток
                    last_gesture = predicted_gesture
                    last_confidence = confidence_value
                    
                    result = {
                        "gesture": predicted_gesture,
                        "confidence": float(confidence_value * 100),
                        "timestamp": time.time()
                    }
                    print(json.dumps(result))
                    sys.stdout.flush()
                    
                    # Блокируем детекцию и запускаем таймер
                    detection_allowed = False
                    buffer.clear()
                    prev_frame_coords = None
                    prediction_history.clear()  # Очищаем историю после успешной детекции
                    
                    cooldown_timer = threading.Thread(target=reset_detection_allowed, daemon=True)
                    cooldown_timer.start()

cap.release()
cv2.destroyAllWindows()
    

    