#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
OptiFine CIT to ItemsAdder Converter
Конвертирует ресурспаки OptiFine CIT в аддоны ItemsAdder для Minecraft 1.21.5+

Интерактивный CLI редактор для пост-обработки сгенерированных предметов
"""

import yaml
import logging
from pathlib import Path
from typing import Dict, List, Any, Optional
from rich.console import Console
from rich.table import Table
from rich.prompt import Prompt, Confirm
from rich.panel import Panel
from rich.text import Text

logger = logging.getLogger(__name__)


class InteractiveEditor:
    """
    Интерактивный CLI редактор для редактирования сгенерированных предметов
    """
    
    def __init__(self, config_path: Path, namespace: str = "converted_pack"):
        self.config_path = config_path
        self.namespace = namespace
        self.console = Console()
        self.config: Dict[str, Any] = {}
        self.items: Dict[str, Dict] = {}
        
        self._load_config()
        
    def _load_config(self) -> None:
        """Загружает config.yml"""
        if not self.config_path.exists():
            logger.error(f"Config файл не найден: {self.config_path}")
            raise FileNotFoundError(f"Config file not found: {self.config_path}")
            
        try:
            with open(self.config_path, 'r', encoding='utf-8') as f:
                self.config = yaml.safe_load(f)
            self.items = self.config.get('items', {})
        except Exception as e:
            logger.error(f"Ошибка загрузки config: {e}")
            raise
    
    def _save_config(self) -> bool:
        """Сохраняет изменения в config.yml"""
        try:
            with open(self.config_path, 'w', encoding='utf-8') as f:
                yaml.dump(self.config, f, allow_unicode=True, 
                         default_flow_style=False, sort_keys=False)
            return True
        except Exception as e:
            logger.error(f"Ошибка сохранения config: {e}")
            return False
    
    def display_items_table(self) -> None:
        """
        Отображает таблицу всех предметов
        """
        table = Table(title="Сгенерированные предметы", show_header=True, header_style="bold magenta")
        
        table.add_column("ID", style="cyan", width=30)
        table.add_column("Display Name", style="green", width=40)
        table.add_column("Type", style="yellow", width=15)
        table.add_column("NBT Match", style="red", width=40, overflow="ellipsis")
        
        for item_id, item_data in self.items.items():
            display_name = item_data.get('display_name', 'N/A')[:38] + '..' if len(item_data.get('display_name', '')) > 40 else item_data.get('display_name', 'N/A')
            
            # Определяем тип
            item_type = "item"
            if 'armor' in item_data:
                item_type = "armor"
            elif 'custom_model_data' in item_data:
                item_type = "cmd"
                
            nbt_name = ""
            if 'properties' in item_data and 'nbt' in item_data['properties']:
                nbt_raw = item_data['properties']['nbt'].get('display', {}).get('Name', '')
                nbt_name = nbt_raw[:38] + '..' if len(nbt_raw) > 40 else nbt_raw
                
            table.add_row(item_id, display_name, item_type, nbt_name)
            
        self.console.print(table)
        self.console.print(f"\nВсего предметов: {len(self.items)}")
    
    def select_item(self) -> Optional[str]:
        """
        Позволяет пользователю выбрать предмет для редактирования
        
        Returns:
            ID выбранного предмета или None
        """
        self.display_items_table()
        
        self.console.print("\n[bold cyan]Выберите предмет для редактирования:[/bold cyan]")
        self.console.print("  - Введите ID предмета")
        self.console.print("  - 'all' для массового редактирования")
        self.console.print("  - 'skip' для пропуска")
        self.console.print("  - 'save & exit' для сохранения и выхода")
        self.console.print("  - 'exit without save' для выхода без сохранения")
        
        choice = Prompt.ask("\nВаш выбор", default="skip")
        
        if choice.lower() in ('skip', ''):
            return None
        elif choice.lower() == 'save & exit':
            if self._save_config():
                self.console.print("[green]Конфигурация сохранена![/green]")
            return 'SAVE_EXIT'
        elif choice.lower() == 'exit without save':
            return 'EXIT_NO_SAVE'
        elif choice.lower() == 'all':
            return 'ALL'
        else:
            if choice in self.items:
                return choice
            else:
                self.console.print(f"[red]Предмет '{choice}' не найден![/red]")
                return None
    
    def edit_item(self, item_id: str) -> bool:
        """
        Редактирует выбранный предмет
        
        Args:
            item_id: ID предмета
            
        Returns:
            True если были внесены изменения
        """
        if item_id not in self.items:
            self.console.print(f"[red]Предмет '{item_id}' не найден![/red]")
            return False
            
        item_data = self.items[item_id]
        modified = False
        
        while True:
            self.console.print(Panel(
                f"[bold cyan]Редактирование: {item_id}[/bold cyan]\n"
                f"Display Name: [green]{item_data.get('display_name', 'N/A')}[/green]\n"
                f"Type: [yellow]{'armor' if 'armor' in item_data else 'item'}[/yellow]",
                title="Информация о предмете"
            ))
            
            self.console.print("\n[bold]Доступные действия:[/bold]")
            actions = [
                "1. Изменить display_name",
                "2. Изменить custom_model_data",
                "3. Изменить NBT свойства",
                "4. Изменить durability",
                "5. Добавить/изменить зачарования (enchants)",
                "6. Изменить параметры брони (armor)",
                "7. Изменить resource_pack настройки",
                "8. Просмотреть полный JSON",
                "9. Назад к выбору предмета"
            ]
            for action in actions:
                self.console.print(f"  {action}")
                
            choice = Prompt.ask("\nДействие", default="9")
            
            if choice == '1':
                new_name = Prompt.ask("Новый display_name", default=item_data.get('display_name', ''))
                if new_name:
                    item_data['display_name'] = new_name
                    modified = True
                    self.console.print("[green]Display Name обновлён![/green]")
                    
            elif choice == '2':
                current_cmd = item_data.get('custom_model_data', 'не установлен')
                new_cmd = Prompt.ask(f"custom_model_data (текущий: {current_cmd})", default=str(current_cmd))
                if new_cmd:
                    try:
                        item_data['custom_model_data'] = int(new_cmd)
                        modified = True
                        self.console.print("[green]Custom Model Data обновлён![/green]")
                    except ValueError:
                        self.console.print("[red]Некорректное число![/red]")
                        
            elif choice == '3':
                self._edit_nbt_properties(item_data)
                modified = True
                
            elif choice == '4':
                new_durability = Prompt.ask("Max Durability (оставьте пустым для удаления)", 
                                           default=str(item_data.get('max_durability', '')))
                if new_durability:
                    try:
                        item_data['max_durability'] = int(new_durability)
                        modified = True
                        self.console.print("[green]Durability обновлён![/green]")
                    except ValueError:
                        self.console.print("[red]Некорректное число![/red]")
                else:
                    if 'max_durability' in item_data:
                        del item_data['max_durability']
                        modified = True
                        
            elif choice == '5':
                self._edit_enchants(item_data)
                modified = True
                
            elif choice == '6':
                if 'armor' in item_data:
                    self._edit_armor(item_data)
                else:
                    if Confirm.ask("Добавить секцию armor?"):
                        item_data['armor'] = {'slot': ['HEAD', 'CHEST', 'LEGS', 'FEET']}
                        self._edit_armor(item_data)
                modified = True
                
            elif choice == '7':
                self._edit_resource_pack(item_data)
                modified = True
                
            elif choice == '8':
                import json
                self.console.print(json.dumps(item_data, indent=2, ensure_ascii=False))
                
            elif choice == '9':
                break
            else:
                self.console.print("[red]Некорректный выбор![/red]")
                
        return modified
    
    def _edit_nbt_properties(self, item_data: Dict) -> None:
        """Редактирует NBT свойства"""
        if 'properties' not in item_data:
            item_data['properties'] = {}
        if 'nbt' not in item_data['properties']:
            item_data['properties']['nbt'] = {'display': {}}
            
        current_name = item_data['properties']['nbt'].get('display', {}).get('Name', '')
        new_name = Prompt.ask("NBT Display Name", default=current_name)
        item_data['properties']['nbt']['display']['Name'] = new_name
        
        # Дополнительные NBT свойства
        if Confirm.ask("Добавить дополнительные NBT свойства?", default=False):
            key = Prompt.ask("NBT ключ (например, Damage)")
            value = Prompt.ask("NBT значение")
            if key and value:
                try:
                    # Пробуем распарсить как число
                    item_data['properties']['nbt'][key] = int(value)
                except ValueError:
                    try:
                        item_data['properties']['nbt'][key] = float(value)
                    except ValueError:
                        item_data['properties']['nbt'][key] = value
                        
    def _edit_enchants(self, item_data: Dict) -> None:
        """Редактирует зачарования"""
        if 'enchants' not in item_data:
            item_data['enchants'] = []
            
        self.console.print(f"\nТекущие зачарования: {item_data['enchants']}")
        
        if Confirm.ask("Добавить зачарование?"):
            enchant_id = Prompt.ask("ID зачарования (например, sharpness)")
            level = Prompt.ask("Уровень", default="1")
            if enchant_id:
                item_data['enchants'].append({enchant_id: int(level)})
                
        if item_data['enchants'] and Confirm.ask("Удалить зачарование?"):
            for i, ench in enumerate(item_data['enchants']):
                self.console.print(f"  {i+1}. {ench}")
            idx = Prompt.ask("Номер для удаления", default="0")
            try:
                idx_int = int(idx)
                if 0 < idx_int <= len(item_data['enchants']):
                    item_data['enchants'].pop(idx_int - 1)
            except ValueError:
                pass
                
    def _edit_armor(self, item_data: Dict) -> None:
        """Редактирует параметры брони"""
        armor = item_data.get('armor', {})
        
        self.console.print(f"\nТекущие настройки брони:")
        self.console.print(f"  Slot: {armor.get('slot', [])}")
        self.console.print(f"  Texture: {armor.get('texture', 'N/A')}")
        
        # Редактирование слотов
        slot_choice = Prompt.ask(
            "Слоты (HEAD/CHEST/LEGS/FEET через запятую)",
            default=','.join(armor.get('slot', []))
        )
        if slot_choice:
            armor['slot'] = [s.strip().upper() for s in slot_choice.split(',')]
            
        # Редактирование текстуры
        tex_choice = Prompt.ask(
            "Текстура брони",
            default=armor.get('texture', '')
        )
        if tex_choice:
            armor['texture'] = tex_choice
            
        # model_3d
        if Confirm.ask("Использовать 3D модель?", default=False):
            model_path = Prompt.ask("Путь к 3D модели (.bbmodel)")
            if model_path:
                armor['model_3d'] = model_path
                
        item_data['armor'] = armor
        
    def _edit_resource_pack(self, item_data: Dict) -> None:
        """Редактирует настройки resource_pack"""
        if 'resource_pack' not in item_data:
            item_data['resource_pack'] = {}
            
        rp = item_data['resource_pack']
        
        self.console.print(f"\nТекущие настройки resource_pack:")
        self.console.print(f"  model_id: {rp.get('model_id', 'auto')}")
        self.console.print(f"  generate: {rp.get('generate', False)}")
        
        new_model_id = Prompt.ask("model_id", default=rp.get('model_id', 'auto'))
        if new_model_id:
            rp['model_id'] = new_model_id
            
        new_generate = Confirm.ask("Генерировать модель?", default=rp.get('generate', False))
        rp['generate'] = new_generate
        
    def run(self) -> bool:
        """
        Запускает интерактивный редактор
        
        Returns:
            True если изменения сохранены
        """
        self.console.print(Panel.fit(
            "[bold magenta]Interactive Editor для ItemsAdder Config[/bold magenta]\n"
            "Используйте этот редактор для тонкой настройки предметов",
            title="Добро пожаловать"
        ))
        
        should_save = False
        
        while True:
            selection = self.select_item()
            
            if selection is None:
                continue
            elif selection == 'SAVE_EXIT':
                should_save = True
                break
            elif selection == 'EXIT_NO_SAVE':
                should_save = False
                break
            elif selection == 'ALL':
                # Массовое редактирование
                for item_id in list(self.items.keys()):
                    self.console.print(f"\n[bold]Редактирование: {item_id}[/bold]")
                    self.edit_item(item_id)
                    
                if Confirm.ask("\nСохранить изменения?"):
                    should_save = True
                break
            elif selection:
                self.edit_item(selection)
                
        if should_save:
            if self._save_config():
                self.console.print("\n[green bold]✓ Конфигурация успешно сохранена![/green bold]")
                return True
            else:
                self.console.print("\n[red bold]✗ Ошибка сохранения![/red bold]")
                return False
        else:
            self.console.print("\n[yellow]Изменения не сохранены[/yellow]")
            return False
