#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
OptiFine CIT to ItemsAdder Converter
Конвертирует ресурспаки OptiFine CIT в аддоны ItemsAdder для Minecraft 1.21.5+

Модуль парсинга .properties файлов
"""

import re
import logging
from pathlib import Path
from typing import Dict, Optional, List, Any

logger = logging.getLogger(__name__)


class PropertiesParser:
    """
    Robust парсер .properties файлов с поддержкой комментариев и различных форматов
    Не использует configparser из-за специфики формата OptiFine
    """
    
    # Цветовые коды Minecraft
    COLOR_CODES = {
        '0': '', '1': '', '2': '', '3': '', '4': '', '5': '', '6': '', '7': '',
        '8': '', '9': '', 'a': '', 'b': '', 'c': '', 'd': '', 'e': '', 'f': '',
        'k': '', 'l': '', 'm': '', 'n': '', 'o': '', 'r': ''
    }
    
    def __init__(self):
        self.properties: Dict[str, str] = {}
        self.comments: List[str] = []
        
    def parse_file(self, filepath: Path) -> Dict[str, str]:
        """
        Парсит .properties файл и возвращает словарь ключ-значение
        
        Args:
            filepath: Путь к файлу
            
        Returns:
            Словарь свойств
        """
        self.properties = {}
        self.comments = []
        
        if not filepath.exists():
            logger.warning(f"Файл не найден: {filepath}")
            return {}
            
        try:
            with open(filepath, 'r', encoding='utf-8') as f:
                for line_num, line in enumerate(f, 1):
                    self._parse_line(line.strip(), line_num, filepath)
                    
        except UnicodeDecodeError:
            # Пробуем другую кодировку
            try:
                with open(filepath, 'r', encoding='latin-1') as f:
                    for line_num, line in enumerate(f, 1):
                        self._parse_line(line.strip(), line_num, filepath)
            except Exception as e:
                logger.error(f"Ошибка чтения файла {filepath}: {e}")
        except Exception as e:
            logger.error(f"Ошибка чтения файла {filepath}: {e}")
            
        return self.properties
    
    def _parse_line(self, line: str, line_num: int, filepath: Path) -> None:
        """
        Парсит одну строку .properties файла
        """
        # Пропускаем пустые строки
        if not line:
            return
            
        # Комментарии
        if line.startswith('#'):
            self.comments.append(line)
            return
            
        # Ищем разделитель = или :
        separator_pos = -1
        for i, char in enumerate(line):
            if char in ('=', ':'):
                separator_pos = i
                break
                
        if separator_pos == -1:
            # Нет разделителя, пропускаем
            logger.debug(f"Строка без разделителя в {filepath}:{line_num}: {line}")
            return
            
        key = line[:separator_pos].strip()
        value = line[separator_pos + 1:].strip()
        
        # Убираем кавычки если есть
        if (value.startswith('"') and value.endswith('"')) or \
           (value.startswith("'") and value.endswith("'")):
            value = value[1:-1]
            
        if key:
            self.properties[key] = value
            
    @staticmethod
    def strip_color_codes(text: str) -> str:
        """
        Удаляет цветовые коды § и & из текста
        
        Args:
            text: Текст с цветовыми кодами
            
        Returns:
            Текст без цветовых кодов
        """
        if not text:
            return text
            
        # Удаляем §X и &X паттерны
        result = re.sub(r'[§&][0-9a-fk-orA-FK-OR]', '', text)
        return result
    
    @staticmethod
    def extract_clean_name(name_value: str) -> str:
        """
        Извлекает чистое имя предмета из NBT name значения
        
        Args:
            name_value: Значение nbt.display.Name или name
            
        Returns:
            Чистое имя для использования как ID
        """
        if not name_value:
            return "unnamed_item"
            
        # Сначала декодируем unicode escape последовательности (\u00A7 -> §)
        try:
            clean = name_value.encode('utf-8').decode('unicode_escape')
        except:
            clean = name_value
            
        # Удаляем цветовые коды
        clean = PropertiesParser.strip_color_codes(clean)
        
        # Удаляем JSON компоненты если есть
        # Например: {"text":"Sword","italic":false}
        if clean.startswith('{') and 'text' in clean:
            match = re.search(r'"text"\s*:\s*"([^"]+)"', clean)
            if match:
                clean = match.group(1)
                
        # Заменяем пробелы на подчёркивания
        clean = clean.replace(' ', '_')
        
        # Удаляем специальные символы, оставляем буквы, цифры, подчёркивания
        clean = re.sub(r'[^a-zA-Z0-9_]', '', clean)
        
        # Приводим к нижнему регистру
        clean = clean.lower()
        
        # Ограничиваем длину
        if len(clean) > 64:
            clean = clean[:64]
            
        if not clean:
            clean = "unnamed_item"
            
        return clean
    
    def get_match_items(self) -> List[str]:
        """
        Получает список matchItems (может быть несколько через пробел)
        
        Returns:
            Список ID предметов
        """
        match_items_str = self.properties.get('matchItems', '')
        if not match_items_str:
            return []
            
        # Разделяем по пробелам или запятым
        items = re.split(r'[\s,]+', match_items_str.strip())
        return [item for item in items if item]
    
    def get_texture_path(self) -> Optional[str]:
        """Получает путь к текстуре"""
        return self.properties.get('texture')
    
    def get_model_path(self) -> Optional[str]:
        """Получает путь к модели"""
        return self.properties.get('model')
    
    def get_type(self) -> Optional[str]:
        """Получает тип (например, armor)"""
        return self.properties.get('type')
    
    def get_nbt_name(self) -> Optional[str]:
        """
        Получает NBT display name
        Проверяет nbt.display.Name и name
        """
        nbt_name = self.properties.get('nbt.display.Name')
        if not nbt_name:
            nbt_name = self.properties.get('name')
        return nbt_name
    
    def get_armor_layer(self) -> Optional[str]:
        """Получает слой брони (layer_1 или layer_2)"""
        return self.properties.get('armor.layer') or self.properties.get('layer')
    
    def has_regex_pattern(self) -> bool:
        """
        Проверяет, содержит ли имя regex паттерн
        """
        nbt_name = self.get_nbt_name()
        if not nbt_name:
            return False
            
        # Проверяем наличие .* или других regex символов
        regex_indicators = ['.*', '.+', '[', ']', '^', '$', '?', '+']
        return any(ind in nbt_name for ind in regex_indicators)
    
    def is_json_component(self) -> bool:
        """
        Проверяет, является ли имя JSON компонентом
        """
        nbt_name = self.get_nbt_name()
        if not nbt_name:
            return False
        return nbt_name.strip().startswith('{')


class CITProperties:
    """
    Класс представляющий распарсенные свойства CIT предмета
    """
    
    def __init__(self, filepath: Path, parser: PropertiesParser):
        self.filepath = filepath
        self.parser = parser
        self.properties = parser.properties
        
        # Извлечённые данные
        self.match_items = parser.get_match_items()
        self.nbt_name = parser.get_nbt_name()
        self.clean_name = parser.extract_clean_name(self.nbt_name) if self.nbt_name else "unnamed"
        self.texture_path = parser.get_texture_path()
        self.model_path = parser.get_model_path()
        self.item_type = parser.get_type()
        self.armor_layer = parser.get_armor_layer()
        
        # Флаги
        self.has_regex = parser.has_regex_pattern()
        self.is_json = parser.is_json_component()
        
    def __repr__(self) -> str:
        return f"CITProperties(name={self.clean_name}, type={self.item_type})"
