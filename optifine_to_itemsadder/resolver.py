#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
OptiFine CIT to ItemsAdder Converter
Конвертирует ресурспаки OptiFine CIT в аддоны ItemsAdder для Minecraft 1.21.5+

Модуль разрешения путей к текстурам и моделям
"""

import json
import shutil
import logging
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Set

logger = logging.getLogger(__name__)


class ResourceResolver:
    """
    Разрешает пути к текстурам и моделям, копирует их в новую структуру
    """
    
    def __init__(self, source_pack_path: Path, target_namespace: str = "converted_pack"):
        self.source_pack = source_pack_path
        self.target_namespace = target_namespace
        self.copied_textures: Set[str] = set()
        self.copied_models: Set[str] = set()
        self.warnings: List[str] = []
        
        # Пути в исходном паке
        self.minecraft_assets = source_pack_path / "assets" / "minecraft"
        self.textures_dir = self.minecraft_assets / "textures"
        self.models_dir = self.minecraft_assets / "models" / "item"
        
    def resolve_texture_path(self, texture_path: str) -> Optional[Path]:
        """
        Разрешает путь к текстуре относительно assets/minecraft/textures/
        
        Args:
            texture_path: Путь из .properties (например, item/sword_custom)
            
        Returns:
            Полный путь к файлу текстуры или None
        """
        if not texture_path:
            return None
            
        # Убираем префиксы если есть
        clean_path = texture_path
        if clean_path.startswith('minecraft:'):
            clean_path = clean_path.replace('minecraft:', '', 1)
        if clean_path.startswith('/'):
            clean_path = clean_path[1:]
            
        # Добавляем расширение если нет
        if not clean_path.endswith('.png'):
            clean_path += '.png'
            
        # Пробуем найти в textures/item/
        possible_paths = [
            self.textures_dir / clean_path,
            self.textures_dir / "item" / clean_path.lstrip("item/"),
            self.textures_dir / clean_path.lstrip("item/"),
        ]
        
        for path in possible_paths:
            if path.exists():
                logger.debug(f"Найдена текстура: {path}")
                return path
                
        # Если не нашли, возвращаем путь относительно textures/item
        return self.textures_dir / "item" / clean_path.lstrip("item/")
    
    def resolve_model_path(self, model_path: str) -> Optional[Path]:
        """
        Разрешает путь к JSON модели
        
        Args:
            model_path: Путь из .properties
            
        Returns:
            Полный путь к JSON модели или None
        """
        if not model_path:
            return None
            
        # Убираем префиксы
        clean_path = model_path
        if clean_path.startswith('minecraft:'):
            clean_path = clean_path.replace('minecraft:', '', 1)
        if clean_path.startswith('/'):
            clean_path = clean_path[1:]
            
        # Добавляем расширение если нет
        if not clean_path.endswith('.json'):
            clean_path += '.json'
            
        # Ищем в models/item/
        model_file = self.models_dir / clean_path.lstrip("item/")
        
        if model_file.exists():
            logger.debug(f"Найдена модель: {model_file}")
            return model_file
            
        return model_file
    
    def extract_textures_from_model(self, model_path: Path) -> List[str]:
        """
        Извлекает все пути к текстурам из JSON модели
        
        Args:
            model_path: Путь к JSON модели
            
        Returns:
            Список путей к текстурам
        """
        textures = []
        
        if not model_path.exists():
            self.warnings.append(f"Модель не найдена: {model_path}")
            return textures
            
        try:
            with open(model_path, 'r', encoding='utf-8') as f:
                model_data = json.load(f)
        except json.JSONDecodeError as e:
            self.warnings.append(f"Ошибка JSON в модели {model_path}: {e}")
            return textures
        except Exception as e:
            self.warnings.append(f"Ошибка чтения модели {model_path}: {e}")
            return textures
            
        # Рекурсивно извлекаем текстуры
        self._extract_textures_recursive(model_data, textures)
        
        return textures
    
    def _extract_textures_recursive(self, data: dict, textures: List[str], visited: Set[str] = None) -> None:
        """
        Рекурсивно извлекает текстуры из JSON структуры
        """
        if visited is None:
            visited = set()
            
        if isinstance(data, dict):
            # Проверяем секцию textures
            if 'textures' in data:
                tex_section = data['textures']
                if isinstance(tex_section, dict):
                    for key, value in tex_section.items():
                        if isinstance(value, str):
                            # Пропускаем уже посещённые (для parent моделей)
                            if value not in visited:
                                visited.add(value)
                                
                                # Если значение - это ссылка на другую модель (#layer0)
                                if value.startswith('#'):
                                    continue
                                    
                                textures.append(value)
                                
                                # Если это полный путь с namespace
                                if ':' in value and not value.startswith('#'):
                                    parts = value.split(':', 1)
                                    if len(parts) == 2:
                                        namespace, path = parts
                                        if namespace != 'minecraft':
                                            # Кастомная текстура из другого namespace
                                            custom_tex = self.source_pack / "assets" / namespace / "textures" / f"{path}.png"
                                            if custom_tex.exists():
                                                textures.append(str(custom_tex))
                                                
            # Рекурсивно обрабатываем parent
            if 'parent' in data and isinstance(data['parent'], str):
                parent_path = self._resolve_parent_model(data['parent'])
                if parent_path and parent_path.exists():
                    try:
                        with open(parent_path, 'r', encoding='utf-8') as f:
                            parent_data = json.load(f)
                        self._extract_textures_recursive(parent_data, textures, visited)
                    except:
                        pass
                        
            # Обрабатываем другие поля
            for key, value in data.items():
                if key not in ('textures', 'parent'):
                    self._extract_textures_recursive(value, textures, visited)
                    
        elif isinstance(data, list):
            for item in data:
                self._extract_textures_recursive(item, textures, visited)
    
    def _resolve_parent_model(self, parent: str) -> Optional[Path]:
        """
        Разрешает путь к parent модели
        """
        if not parent or parent.startswith('#'):
            return None
            
        clean_path = parent
        if clean_path.startswith('minecraft:'):
            clean_path = clean_path.replace('minecraft:', '', 1)
        if not clean_path.endswith('.json'):
            clean_path += '.json'
            
        return self.minecraft_assets / "models" / clean_path
    
    def copy_texture(self, source_path: Path, target_base: Path, relative_path: str) -> Optional[str]:
        """
        Копирует текстуру в целевую директорию
        
        Args:
            source_path: Исходный путь к текстуре
            target_base: Базовая директория назначения
            relative_path: Относительный путь (например, item/sword_custom.png)
            
        Returns:
            Новый путь относительно namespace или None
        """
        if not source_path.exists():
            self.warnings.append(f"Текстура не найдена: {source_path}")
            return None
            
        # Создаём целевой путь
        target_path = target_base / "textures" / relative_path
        target_path.parent.mkdir(parents=True, exist_ok=True)
        
        try:
            shutil.copy2(source_path, target_path)
            self.copied_textures.add(str(relative_path))
            logger.debug(f"Скопирована текстура: {relative_path}")
            return f"{self.target_namespace}:textures/{relative_path}"
        except Exception as e:
            self.warnings.append(f"Ошибка копирования текстуры {source_path}: {e}")
            return None
    
    def copy_model(self, source_path: Path, target_base: Path, 
                   texture_mapping: Dict[str, str]) -> Optional[Dict]:
        """
        Копирует JSON модель и обновляет пути к текстурам
        
        Args:
            source_path: Исходный путь к модели
            target_base: Базовая директория назначения
            texture_mapping: Маппинг старых путей к новым
            
        Returns:
            Обновлённая JSON структура модели или None
        """
        if not source_path.exists():
            self.warnings.append(f"Модель не найдена: {source_path}")
            return None
            
        try:
            with open(source_path, 'r', encoding='utf-8') as f:
                model_data = json.load(f)
        except Exception as e:
            self.warnings.append(f"Ошибка чтения модели {source_path}: {e}")
            return None
            
        # Обновляем текстуры
        if 'textures' in model_data and isinstance(model_data['textures'], dict):
            for key, value in model_data['textures'].items():
                if isinstance(value, str) and not value.startswith('#'):
                    # Пробуем заменить путь
                    for old_path, new_path in texture_mapping.items():
                        if old_path in value or value.endswith(old_path.replace('.png', '')):
                            # Извлекаем имя текстуры без пути
                            tex_name = value.split('/')[-1].replace('.png', '')
                            model_data['textures'][key] = f"{self.target_namespace}:item/{tex_name}"
                            break
                            
        # Обновляем parent если есть
        if 'parent' in model_data and isinstance(model_data['parent'], str):
            parent = model_data['parent']
            if not parent.startswith(self.target_namespace):
                # Заменяем parent на стандартный minecraft или кастомный
                if parent.startswith('minecraft:item/'):
                    # Оставляем как есть, это стандартная модель
                    pass
                else:
                    model_data['parent'] = f"minecraft:item/generated"
                    
        return model_data
    
    def get_target_texture_path(self, original_path: str) -> str:
        """
        Преобразует оригинальный путь текстуры в путь для ItemsAdder
        
        Args:
            original_path: Оригинальный путь (например, item/sword_custom)
            
        Returns:
            Путь для ItemsAdder
        """
        clean_path = original_path
        if clean_path.startswith('item/'):
            clean_path = clean_path[5:]
        if clean_path.startswith('/'):
            clean_path = clean_path[1:]
            
        return f"item/{clean_path}"
