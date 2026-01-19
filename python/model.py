import torch
import torch.nn as nn
from torch.nn.utils.rnn import pack_padded_sequence, pad_packed_sequence

class GestureLSTM(nn.Module):
    def __init__(self, input_size=126, hidden_size=64, num_layers=2, num_classes=2, dropout=0.3):
        super().__init__()
        self.hidden_size = hidden_size
        self.num_layers = num_layers
        
        self.lstm = nn.LSTM(
            input_size=input_size,
            hidden_size=hidden_size,
            num_layers=num_layers,
            batch_first=True,
            dropout=dropout if num_layers > 1 else 0
        )
        self.fc = nn.Linear(hidden_size, num_classes)

    def forward(self, x, lengths=None):
        """
        Forward pass с поддержкой переменной длины последовательностей
        
        Args:
            x: (batch, max_seq_len, input_size) - тензор с padding
            lengths: (batch,) - фактические длины последовательностей (без padding)
                    Если None, используется вся последовательность
        """
        batch_size = x.size(0)
        
        # Если длины не указаны, используем максимальную длину для всех
        if lengths is None:
            lengths = torch.full((batch_size,), x.size(1), dtype=torch.long, device=x.device)
        
        # Упаковываем последовательности для эффективной обработки
        # pack_padded_sequence автоматически игнорирует padding
        packed_input = pack_padded_sequence(
            x, 
            lengths.cpu(),  # Должно быть на CPU для pack_padded_sequence
            batch_first=True, 
            enforce_sorted=False
        )
        
        # Пропускаем через LSTM
        packed_output, (hidden, cell) = self.lstm(packed_input)
        
        # Распаковываем (опционально, если нужны все выходы)
        # output, _ = pad_packed_sequence(packed_output, batch_first=True)
        
        # Берем последний скрытый слой из всех слоев
        # hidden shape: (num_layers, batch, hidden_size)
        last_hidden = hidden[-1]  # Берем последний слой: (batch, hidden_size)
        
        # Альтернативный вариант: берем последний выход из packed_output
        # Это более точно, так как учитывает реальную длину последовательности
        output, output_lengths = pad_packed_sequence(packed_output, batch_first=True)
        
        # Берем последний валидный выход для каждой последовательности
        # Используем lengths для индексации
        idx = (lengths - 1).unsqueeze(1).expand(-1, self.hidden_size).unsqueeze(1)
        last_output = output.gather(1, idx).squeeze(1)  # (batch, hidden_size)
        
        # Используем last_output вместо last_hidden (более точно)
        out = self.fc(last_output)
        return out