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
DETECTION_COOLDOWN = 1.0  # Пауза ПОСЛЕ успешной детекции

# НАСТРОЙКИ СТАБИЛИЗАЦИИ
STABILIZATION_CONSECUTIVE_FRAMES = 4  # Количество последовательных одинаковых предсказаний
CONFIDENCE_THRESHOLD_STABLE = 0.95  # Порог уверенности для стабилизации

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

def invert_rotate_gestures(gesture):
    """
    Инвертирует ТОЛЬКО жесты поворотов из-за зеркального отображения камеры
    Свайпы оставляем без изменений
    """
    if gesture == "rotateLeft":
        return "rotateRight"
    elif gesture == "rotateRight":
        return "rotateLeft"
    else:
        return gesture  # Все остальные жесты (включая свайпы) остаются как есть

# MediaPipe
mp_hands = mp.solutions.hands
hands = mp_hands.Hands(static_image_mode=False, max_num_hands=1, min_detection_confidence=0.5)

# Буфер кадров для модели
buffer = deque(maxlen=SEQ_LENGTH)
prev_frame_coords = None

# Структуры для стабилизации
consecutive_count = 0  # Счетчик последовательных одинаковых предсказаний
current_gesture_candidate = None  # Кандидат на жест

# Флаги для контроля детекции
detection_allowed = True
cooldown_timer = None
last_detection_time = 0

def reset_detection_allowed():
    """Сбрасывает флаг разрешения детекции через заданное время"""
    global detection_allowed, buffer, prev_frame_coords
    global consecutive_count, current_gesture_candidate
    
    time.sleep(DETECTION_COOLDOWN)
    
    # ВОССТАНАВЛИВАЕМ все буферы и состояния
    buffer.clear()
    prev_frame_coords = None
    consecutive_count = 0
    current_gesture_candidate = None
    
    # Разрешаем детекцию заново
    detection_allowed = True

def check_consecutive_predictions(predicted_gesture, confidence_value):
    """
    Проверяет, есть ли достаточное количество последовательных одинаковых предсказаний
    """
    global consecutive_count, current_gesture_candidate
    
    if confidence_value < CONFIDENCE_THRESHOLD_STABLE:
        consecutive_count = 0
        current_gesture_candidate = None
        return False
    
    if predicted_gesture.lower() == "idle" and IGNORE_IDLE:
        consecutive_count = 0
        current_gesture_candidate = None
        return False
    
    # Корректируем ТОЛЬКО повороты для проверки стабильности
    corrected_gesture = invert_rotate_gestures(predicted_gesture)
    
    # Если жесты совпадают, увеличиваем счетчик
    if current_gesture_candidate == corrected_gesture:
        consecutive_count += 1
    else:
        # Начинаем отсчет для нового жеста
        current_gesture_candidate = corrected_gesture
        consecutive_count = 1
    
    # Проверяем, достигли ли мы нужного количества последовательных кадров
    if consecutive_count >= STABILIZATION_CONSECUTIVE_FRAMES:
        return True
    
    return False

cap = cv2.VideoCapture(0)

while cap.isOpened():
    ret, frame = cap.read()
    if not ret:
        continue

    frame = cv2.flip(frame, 1)  # Горизонтальное отражение (зеркало)
    rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    results = hands.process(rgb)

    # Обновляем буфер для модели (ВСЕГДА, независимо от detection_allowed)
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

    # Основная логика детекции с улучшенной системой пауз
    if detection_allowed and hand_detected and len(buffer) == SEQ_LENGTH:
        # Предсказание для текущего кадра
        seq = torch.tensor(np.array(buffer, dtype=np.float32), dtype=torch.float32).unsqueeze(0).to(device)
        with torch.no_grad():
            lengths = torch.tensor([SEQ_LENGTH], dtype=torch.long).to(device)
            output = model(seq, lengths)
            probabilities = F.softmax(output, dim=1)
            confidence, pred = torch.max(probabilities, dim=1)
            confidence_value = confidence.item()
            pred_class = pred.item()
            predicted_gesture = label_encoder.inverse_transform([pred_class])[0]
            
            # Проверяем базовые условия
            if confidence_value < CONFIDENCE_THRESHOLD:
                continue
            
            if IGNORE_IDLE and predicted_gesture.lower() == "idle":
                continue
            
            # Проверяем последовательные предсказания (система стабилизации)
            if check_consecutive_predictions(predicted_gesture, confidence_value):
                # УСПЕШНАЯ ДЕТЕКЦИЯ!
                last_detection_time = time.time()
                
                # Корректируем ТОЛЬКО повороты перед выводом
                corrected_gesture = invert_rotate_gestures(predicted_gesture)
                
                # Выводим результат в JSON формате
                result = {
                    "gesture": corrected_gesture,
                    "confidence": float(confidence_value * 100),
                    "timestamp": time.time()
                }
                print(json.dumps(result))
                sys.stdout.flush()
                
                # Блокируем детекцию на время коолдауна
                detection_allowed = False
                
                # Запускаем таймер разблокировки
                cooldown_timer = threading.Thread(target=reset_detection_allowed, daemon=True)
                cooldown_timer.start()
    
    # Обработка нажатия 'q' для выхода
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

cap.release()
cv2.destroyAllWindows()