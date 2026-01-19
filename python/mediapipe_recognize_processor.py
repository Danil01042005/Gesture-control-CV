import os
import mediapipe as mp
import numpy as np
import cv2
import joblib
import json
import sys
import base64
import time


class GestureRecognizer:
    def __init__(self, model_path=None):
        if model_path is None:
            script_dir = os.path.dirname(os.path.abspath(__file__))
            model_path = os.path.join(script_dir, 'gesture_classifier_rf.pkl')
        self.mp_hands = mp.solutions.hands
        self.hands = self.mp_hands.Hands(
            static_image_mode=False,
            max_num_hands=1,
            min_detection_confidence=0.5,
            min_tracking_confidence=0.5
        )
        self.mp_draw = mp.solutions.drawing_utils
        
        self.model = joblib.load(model_path)
        print(f"Модель загружена: {model_path}")
        
    def extract_landmarks(self, hand_landmarks):
        """Извлекает координаты из landmarks"""
        landmarks = []
        for landmark in hand_landmarks.landmark:
            landmarks.extend([landmark.x, landmark.y, landmark.z])
        return np.array(landmarks).reshape(1, -1)

    def frame_to_base64(self, frame, quality=50):
        """Конвертация кадра в base64 строку"""
        # Уменьшаем размер для быстрой передачи
        small_frame = cv2.resize(frame, (320, 240))
        
        # Кодируем в JPEG с низким качеством для скорости
        encode_param = [int(cv2.IMWRITE_JPEG_QUALITY), quality]
        _, buffer = cv2.imencode('.jpg', small_frame, encode_param)
        
        # Конвертируем в base64
        jpg_as_text = base64.b64encode(buffer).decode('utf-8')
        return jpg_as_text
    
    def recognize(self):
        """Распознавание жестов в реальном времени"""
        cap = cv2.VideoCapture(0)
        
        print("Запущено распознавание жестов")
        print("Нажмите 'q' для выхода")
        
        while cap.isOpened():
            success, frame = cap.read()
            if not success:
                continue
            
            frame = cv2.flip(frame, 1)
            frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            results = self.hands.process(frame_rgb)
            
            if results.multi_hand_landmarks:
                for hand_landmarks in results.multi_hand_landmarks:

                    self.mp_draw.draw_landmarks(
                        frame, hand_landmarks, self.mp_hands.HAND_CONNECTIONS)
                    

                    landmarks = self.extract_landmarks(hand_landmarks)
                    

                    gesture = self.model.predict(landmarks)[0]
                    

                    if hasattr(self.model, 'predict_proba'):
                        proba = self.model.predict_proba(landmarks)[0]
                        confidence = max(proba) * 100

                        result = {
                            "gesture": gesture,
                            "confidence": float(confidence),
                            "timestamp": time.time()
                        }
                        print(json.dumps(result))
                        sys.stdout.flush()
                        
                    #     # Отображение результата
                    #     cv2.putText(frame, f"Жест: {gesture}", (10, 50),
                    #                cv2.FONT_HERSHEY_SIMPLEX, 1.5, (0, 255, 0), 3)
                    #     cv2.putText(frame, f"Уверенность: {confidence:.1f}%", (10, 100),
                    #                cv2.FONT_HERSHEY_SIMPLEX, 0.8, (255, 255, 255), 2)
                    # else:
                    #     cv2.putText(frame, f"Жест: {gesture}", (10, 50),
                    #                cv2.FONT_HERSHEY_SIMPLEX, 1.5, (0, 255, 0), 3)
            
            # cv2.imshow('Распознавание жестов', frame)
            
            # if cv2.waitKey(1) & 0xFF == ord('q'):
            #     break
        
        cap.release()
        cv2.destroyAllWindows()

# Использование
if __name__ == "__main__":
    script_dir = os.path.dirname(os.path.abspath(__file__))
    model_path = os.path.join(script_dir, 'gesture_classifier_rf.pkl')

    recognizer = GestureRecognizer(model_path)
    recognizer.recognize()
    