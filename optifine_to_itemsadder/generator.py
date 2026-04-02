#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
OptiFine CIT to ItemsAdder Converter
Конвертирует ресурспаки OptiFine CIT в аддоны ItemsAdder для Minecraft 1.21.5+

Модуль генерации YAML конфигов ItemsAdder
"""

import yaml
import json
import logging
from pathlib import Path
from typing import Dict, List, Optional, Any
from datetime import datetime

from parser import CITProperties, PropertiesParser
from resolver import ResourceResolver

logger = logging.getLogger(__name__)


class ItemsAdderGenerator:
    """
    Генерирует конфигурационные файлы ItemsAdder из распарсенных CIT свойств
    """
    
    # Шаблон базового config.yml
    BASE_CONFIG_TEMPLATE = {
        'info': {
            'namespace': 'converted_pack',
            'author': 'OptiFine Converter',
            'version': '1.0.0',
            'description': 'Converted from OptiFine CIT pack'
        },
        'items': {}
    }
    
    def __init__(self, output_dir: Path, namespace: str = "converted_pack"):
        self.output_dir = output_dir
        self.namespace = namespace
        self.resolver: Optional[ResourceResolver] = None
        self.generated_items: List[Dict[str, Any]] = []
        
    def set_resolver(self, resolver: ResourceResolver) -> None:
        """Устанавливает resolver для работы с ресурсами"""
        self.resolver = resolver
        
    def generate_item_config(self, cit_props: CITProperties) -> Dict[str, Any]:
        """
        Генерирует конфигурацию предмета ItemsAdder из CIT свойств
        
        Args:
            cit_props: Распарсенные свойства CIT
            
        Returns:
            Словарь конфигурации предмета
        """
        item_id = cit_props.clean_name
        
        # Базовая структура предмета
        item_config = {
            'display_name': self._format_display_name(cit_props.nbt_name),
            'resource_pack': {
                'model_id': 'auto',
                'generate': False
            },
            'properties': {
                'nbt': {
                    'display': {
                        'Name': cit_props.nbt_name
                    }
                }
            }
        }
        
        # Обработка брони
        if cit_props.item_type == 'armor' or cit_props.armor_layer:
            item_config = self._generate_armor_config(cit_props, item_config)
        else:
            # Обработка обычных предметов
            item_config = self._generate_item_config(cit_props, item_config)
            
        # Предупреждения о regex/JSON
        if cit_props.has_regex:
            logger.warning(f"Предмет '{item_id}' содержит regex паттерн в имени")
            item_config['_warning'] = 'Contains regex pattern in name'
            
        if cit_props.is_json:
            logger.warning(f"Предмет '{item_id}' содержит JSON компонент имени")
            
        return item_config
    
    def _format_display_name(self, nbt_name: Optional[str]) -> str:
        """
        Форматирует display_name для ItemsAdder
        """
        if not nbt_name:
            return "Unnamed Item"
            
        # Для ItemsAdder используем формат с цветовыми кодами
        # Заменяем § на &# для совместимости (опционально)
        return nbt_name
    
    def _generate_item_config(self, cit_props: CITProperties, 
                               base_config: Dict) -> Dict[str, Any]:
        """
        Генерирует конфиг для обычного предмета
        """
        config = base_config.copy()
        
        # Если есть модель, пытаемся её обработать
        if cit_props.model_path and self.resolver:
            model_source = self.resolver.resolve_model_path(cit_props.model_path)
            if model_source and model_source.exists():
                # Извлекаем текстуры из модели
                textures = self.resolver.extract_textures_from_model(model_source)
                
                # Копируем текстуры
                texture_mapping = {}
                for tex_path in textures:
                    resolved = self.resolver.resolve_texture_path(tex_path)
                    if resolved and resolved.exists():
                        target_rel = self.resolver.get_target_texture_path(tex_path)
                        new_path = self.resolver.copy_texture(
                            resolved, 
                            self.output_dir / "assets",
                            target_rel
                        )
                        if new_path:
                            texture_mapping[tex_path] = new_path
                            
                # Копируем и обновляем модель
                updated_model = self.resolver.copy_model(
                    model_source,
                    self.output_dir / "assets",
                    texture_mapping
                )
                
                if updated_model:
                    # Сохраняем модель
                    model_filename = f"{cit_props.clean_name}.json"
                    model_target = self.output_dir / "assets" / self.namespace / "models" / "item" / model_filename
                    model_target.parent.mkdir(parents=True, exist_ok=True)
                    
                    with open(model_target, 'w', encoding='utf-8') as f:
                        json.dump(updated_model, f, indent=2)
                        
                    config['resource_pack']['model_id'] = f"{self.namespace}:{cit_props.clean_name}"
                    
        # Если есть только текстура без модели
        elif cit_props.texture_path and self.resolver:
            tex_source = self.resolver.resolve_texture_path(cit_props.texture_path)
            if tex_source and tex_source.exists():
                target_rel = self.resolver.get_target_texture_path(cit_props.texture_path)
                self.resolver.copy_texture(
                    tex_source,
                    self.output_dir / "assets",
                    target_rel
                )
                
        return config
    
    def _generate_armor_config(self, cit_props: CITProperties,
                                base_config: Dict) -> Dict[str, Any]:
        """
        Генерирует конфиг для брони
        """
        config = base_config.copy()
        
        # Добавляем секцию armor
        armor_config = {
            'slot': ['HEAD', 'CHEST', 'LEGS', 'FEET']  # По умолчанию все слоты
        }
        
        # Определяем тип брони по matchItems
        if cit_props.match_items:
            slot_mapping = {
                'leather_helmet': ['HEAD'],
                'chainmail_helmet': ['HEAD'],
                'iron_helmet': ['HEAD'],
                'golden_helmet': ['HEAD'],
                'diamond_helmet': ['HEAD'],
                'netherite_helmet': ['HEAD'],
                'leather_chestplate': ['CHEST'],
                'chainmail_chestplate': ['CHEST'],
                'iron_chestplate': ['CHEST'],
                'golden_chestplate': ['CHEST'],
                'diamond_chestplate': ['CHEST'],
                'netherite_chestplate': ['CHEST'],
                'leather_leggings': ['LEGS'],
                'chainmail_leggings': ['LEGS'],
                'iron_leggings': ['LEGS'],
                'golden_leggings': ['LEGS'],
                'diamond_leggings': ['LEGS'],
                'netherite_leggings': ['LEGS'],
                'leather_boots': ['FEET'],
                'chainmail_boots': ['FEET'],
                'iron_boots': ['FEET'],
                'golden_boots': ['FEET'],
                'diamond_boots': ['FEET'],
                'netherite_boots': ['FEET'],
            }
            
            for item_id in cit_props.match_items:
                if item_id in slot_mapping:
                    armor_config['slot'] = slot_mapping[item_id]
                    break
                    
        # Обработка текстур слоёв
        if cit_props.armor_layer:
            layer_num = cit_props.armor_layer
            if self.resolver and cit_props.texture_path:
                tex_source = self.resolver.resolve_texture_path(cit_props.texture_path)
                if tex_source and tex_source.exists():
                    # Для брони текстуры обычно в entity/armor/
                    target_rel = f"entity/armor/{cit_props.clean_name}_{layer_num}.png"
                    self.resolver.copy_texture(
                        tex_source,
                        self.output_dir / "assets",
                        target_rel
                    )
                    armor_config['texture'] = f"{self.namespace}:entity/armor/{cit_props.clean_name}_{layer_num}"
                    
        config['armor'] = armor_config
        
        return config
    
    def generate_full_pack(self, cit_properties_list: List[CITProperties],
                           assign_cmd: bool = False) -> Path:
        """
        Генерирует полный пак ItemsAdder
        
        Args:
            cit_properties_list: Список распарсенных CIT свойств
            assign_cmd: Если True, генерировать custom_model_data
            
        Returns:
            Путь к созданному аддону
        """
        addon_dir = self.output_dir / "items_packs" / self.namespace
        addon_dir.mkdir(parents=True, exist_ok=True)
        
        # Создаём структуру директорий
        (addon_dir / "assets").mkdir(exist_ok=True)
        
        # Инициализируем config
        config = self.BASE_CONFIG_TEMPLATE.copy()
        config['info']['namespace'] = self.namespace
        config['items'] = {}
        
        # Генерируем каждый предмет
        for cit_props in cit_properties_list:
            item_id = cit_props.clean_name
            
            # Проверяем уникальность ID
            base_id = item_id
            counter = 1
            while item_id in config['items']:
                item_id = f"{base_id}_{counter}"
                counter += 1
                
            item_config = self.generate_item_config(cit_props)
            
            # Если assign_cmd, добавляем custom_model_data
            if assign_cmd:
                item_config['custom_model_data'] = counter + 1000
                
            config['items'][item_id] = item_config
            self.generated_items.append({
                'id': item_id,
                'original_name': cit_props.nbt_name,
                'config': item_config
            })
            
        # Сохраняем config.yml
        config_path = addon_dir / "config.yml"
        with open(config_path, 'w', encoding='utf-8') as f:
            yaml.dump(config, f, allow_unicode=True, default_flow_style=False, sort_keys=False)
            
        logger.info(f"Сгенерирован config.yml: {config_path}")
        
        # Сохраняем метаданные конвертации
        metadata = {
            'converted_at': datetime.now().isoformat(),
            'source_pack': str(self.resolver.source_pack) if self.resolver else 'unknown',
            'items_count': len(self.generated_items),
            'warnings': self.resolver.warnings if self.resolver else []
        }
        
        metadata_path = addon_dir / "conversion_metadata.json"
        with open(metadata_path, 'w', encoding='utf-8') as f:
            json.dump(metadata, f, indent=2, ensure_ascii=False)
            
        return addon_dir
    
    def get_generated_items(self) -> List[Dict[str, Any]]:
        """Возвращает список сгенерированных предметов"""
        return self.generated_items
    
    def update_item_config(self, item_id: str, updates: Dict[str, Any]) -> bool:
        """
        Обновляет конфигурацию конкретного предмета в config.yml
        
        Args:
            item_id: ID предмета
            updates: Словарь обновлений
            
        Returns:
            True если успешно
        """
        config_path = self.output_dir / "items_packs" / self.namespace / "config.yml"
        
        if not config_path.exists():
            logger.error(f"Config файл не найден: {config_path}")
            return False
            
        try:
            with open(config_path, 'r', encoding='utf-8') as f:
                config = yaml.safe_load(f)
                
            if 'items' not in config or item_id not in config['items']:
                logger.error(f"Предмет '{item_id}' не найден в конфиге")
                return False
                
            # Рекурсивное обновление
            self._deep_update(config['items'][item_id], updates)
            
            # Сохраняем обратно
            with open(config_path, 'w', encoding='utf-8') as f:
                yaml.dump(config, f, allow_unicode=True, default_flow_style=False, sort_keys=False)
                
            logger.info(f"Обновлён предмет: {item_id}")
            return True
            
        except Exception as e:
            logger.error(f"Ошибка обновления предмета {item_id}: {e}")
            return False
    
    def _deep_update(self, base_dict: Dict, update_dict: Dict) -> None:
        """Рекурсивно обновляет словарь"""
        for key, value in update_dict.items():
            if isinstance(value, dict) and key in base_dict and isinstance(base_dict[key], dict):
                self._deep_update(base_dict[key], value)
            else:
                base_dict[key] = value
