#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
OptiFine CIT to ItemsAdder Converter
Конвертирует ресурспаки OptiFine CIT в аддоны ItemsAdder для Minecraft 1.21.5+

Главный модуль конвертации - объединяет все компоненты
"""

import sys
import logging
import argparse
from pathlib import Path
from datetime import datetime
from typing import List, Optional

from rich.console import Console
from rich.panel import Panel
from rich.progress import Progress, SpinnerColumn, TextColumn
from rich.logging import RichHandler

from parser import PropertiesParser, CITProperties
from resolver import ResourceResolver
from generator import ItemsAdderGenerator
from cli_editor import InteractiveEditor


# Настройка логирования
def setup_logging(log_file: Optional[str] = None) -> logging.Logger:
    """
    Настраивает логирование в консоль и файл
    
    Args:
        log_file: Путь к файлу лога (опционально)
        
    Returns:
        Настроенный logger
    """
    logger = logging.getLogger("optifine_converter")
    logger.setLevel(logging.DEBUG)
    
    # Консольный handler с rich
    console_handler = RichHandler(
        rich_tracebacks=True,
        show_time=False,
        show_path=False
    )
    console_handler.setLevel(logging.INFO)
    logger.addHandler(console_handler)
    
    # Файловый handler
    if log_file:
        file_handler = logging.FileHandler(log_file, encoding='utf-8')
        file_handler.setLevel(logging.DEBUG)
        formatter = logging.Formatter(
            '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
        )
        file_handler.setFormatter(formatter)
        logger.addHandler(file_handler)
        
    return logger


class OptiFineConverter:
    """
    Основной класс конвертера
    Объединяет парсинг, разрешение путей и генерацию
    """
    
    def __init__(self, source_pack_path: Path, output_dir: Path,
                 namespace: str = "converted_pack", 
                 assign_cmd: bool = False,
                 interactive: bool = True):
        self.source_pack = source_pack_path
        self.output_dir = output_dir
        self.namespace = namespace
        self.assign_cmd = assign_cmd
        self.interactive = interactive
        
        self.logger = logging.getLogger("optifine_converter")
        self.console = Console()
        
        # Компоненты
        self.parser = PropertiesParser()
        self.resolver: Optional[ResourceResolver] = None
        self.generator: Optional[ItemsAdderGenerator] = None
        
        # Результаты
        self.cit_properties: List[CITProperties] = []
        self.warnings: List[str] = []
        self.errors: List[str] = []
        
    def validate_source_pack(self) -> bool:
        """
        Проверяет валидность исходного ресурспака
        
        Returns:
            True если пак валиден
        """
        required_files = [
            self.source_pack / "pack.mcmeta",
            self.source_pack / "assets"
        ]
        
        for path in required_files:
            if not path.exists():
                self.errors.append(f"Отсутствует обязательный элемент: {path}")
                return False
                
        # Проверяем наличие CIT директории
        cit_dir = self.source_pack / "assets" / "minecraft" / "optifine" / "cit"
        if not cit_dir.exists():
            self.errors.append(f"Директория CIT не найдена: {cit_dir}")
            return False
            
        return True
    
    def scan_cit_files(self) -> List[Path]:
        """
        Сканирует директорию CIT и находит все .properties файлы
        
        Returns:
            Список путей к .properties файлам
        """
        cit_dir = self.source_pack / "assets" / "minecraft" / "optifine" / "cit"
        properties_files = []
        
        if not cit_dir.exists():
            return properties_files
            
        # Рекурсивный поиск .properties файлов
        for props_file in cit_dir.rglob("*.properties"):
            properties_files.append(props_file)
            
        self.logger.info(f"Найдено {len(properties_files)} .properties файлов")
        return properties_files
    
    def parse_cit_files(self, files: List[Path]) -> List[CITProperties]:
        """
        Парсит все найденные .properties файлы
        
        Args:
            files: Список путей к файлам
            
        Returns:
            Список распарсенных CIT свойств
        """
        cit_props_list = []
        
        with Progress(
            SpinnerColumn(),
            TextColumn("[progress.description]{task.description}"),
            console=self.console
        ) as progress:
            task = progress.add_task("Парсинг файлов...", total=len(files))
            
            for filepath in files:
                try:
                    props = self.parser.parse_file(filepath)
                    if props:  # Если есть свойства
                        cit_props = CITProperties(filepath, self.parser)
                        
                        # Предупреждения о regex/JSON
                        if cit_props.has_regex:
                            warning = f"[yellow]ВНИМАНИЕ[/yellow]: {filepath.name} содержит regex паттерн"
                            self.warnings.append(warning)
                            self.logger.warning(warning)
                            
                        if cit_props.is_json:
                            warning = f"[yellow]ВНИМАНИЕ[/yellow]: {filepath.name} содержит JSON компонент"
                            self.warnings.append(warning)
                            self.logger.warning(warning)
                            
                        cit_props_list.append(cit_props)
                        
                except Exception as e:
                    error = f"Ошибка парсинга {filepath}: {e}"
                    self.errors.append(error)
                    self.logger.error(error)
                    
                progress.advance(task)
                
        return cit_props_list
    
    def convert(self) -> bool:
        """
        Запускает процесс конвертации
        
        Returns:
            True если конвертация успешна
        """
        self.console.print(Panel.fit(
            "[bold blue]OptiFine CIT → ItemsAdder Converter[/bold blue]\n"
            f"Source: {self.source_pack}\n"
            f"Output: {self.output_dir}\n"
            f"Namespace: {self.namespace}",
            title="Конвертация"
        ))
        
        # Валидация
        if not self.validate_source_pack():
            self.console.print("[red]✗ Исходный пак невалиден![/red]")
            for error in self.errors:
                self.console.print(f"  [red]- {error}[/red]")
            return False
            
        self.console.print("[green]✓ Исходный пак валиден[/green]")
        
        # Сканирование
        self.console.print("\n[bold]Сканирование CIT файлов...[/bold]")
        properties_files = self.scan_cit_files()
        
        if not properties_files:
            self.console.print("[yellow]⚠ .properties файлы не найдены![/yellow]")
            return False
            
        self.console.print(f"[green]✓ Найдено {len(properties_files)} файлов[/green]")
        
        # Парсинг
        self.console.print("\n[bold]Парсинг .properties файлов...[/bold]")
        self.cit_properties = self.parse_cit_files(properties_files)
        
        if not self.cit_properties:
            self.console.print("[red]✗ Не удалось распарсить ни одного файла![/red]")
            return False
            
        self.console.print(f"[green]✓ Распарсено {len(self.cit_properties)} предметов[/green]")
        
        # Инициализация resolver и generator
        self.resolver = ResourceResolver(self.source_pack, self.namespace)
        self.generator = ItemsAdderGenerator(self.output_dir, self.namespace)
        self.generator.set_resolver(self.resolver)
        
        # Генерация
        self.console.print("\n[bold]Генерация ItemsAdder аддона...[/bold]")
        
        try:
            addon_path = self.generator.generate_full_pack(
                self.cit_properties,
                assign_cmd=self.assign_cmd
            )
            
            self.console.print(f"[green]✓ Аддон сгенерирован: {addon_path}[/green]")
            
            # Отчёт о предупреждениях
            if self.resolver.warnings:
                self.console.print(f"\n[yellow]⚠ Предупреждений: {len(self.resolver.warnings)}[/yellow]")
                for warn in self.resolver.warnings[:10]:  # Показываем первые 10
                    self.console.print(f"  [yellow]- {warn}[/yellow]")
                if len(self.resolver.warnings) > 10:
                    self.console.print(f"  [yellow]... и ещё {len(self.resolver.warnings) - 10}[/yellow]")
                    
            # Интерактивный редактор
            if self.interactive:
                self.console.print("\n[bold magenta]Запуск интерактивного редактора...[/bold magenta]")
                config_path = addon_path / "config.yml"
                
                try:
                    editor = InteractiveEditor(config_path, self.namespace)
                    editor.run()
                except FileNotFoundError as e:
                    self.logger.error(f"Не удалось запустить редактор: {e}")
                except Exception as e:
                    self.logger.error(f"Ошибка редактора: {e}")
                    
            return True
            
        except Exception as e:
            self.logger.error(f"Ошибка генерации: {e}")
            return False
    
    def print_summary(self) -> None:
        """Выводит сводку конвертации"""
        self.console.print("\n" + "="*50)
        self.console.print("[bold]СВОДКА КОНВЕРТАЦИИ[/bold]")
        self.console.print("="*50)
        self.console.print(f"Предметов конвертировано: {len(self.cit_properties)}")
        self.console.print(f"Ошибок: {len(self.errors)}")
        self.console.print(f"Предупреждений: {len(self.warnings)}")
        
        if self.generator:
            items = self.generator.get_generated_items()
            self.console.print(f"\n[bold]Сгенерированные предметы:[/bold]")
            for item in items[:20]:  # Показываем первые 20
                self.console.print(f"  • {item['id']}: {item['original_name'][:50]}")
            if len(items) > 20:
                self.console.print(f"  ... и ещё {len(items) - 20}")


def main():
    """Точка входа CLI"""
    parser = argparse.ArgumentParser(
        description="OptiFine CIT to ItemsAdder Converter",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Примеры использования:
  python converter.py /path/to/optifine_pack
  python converter.py /path/to/pack -o ./output -n my_namespace
  python converter.py /path/to/pack --assign-cmd --no-interactive
        """
    )
    
    parser.add_argument(
        "source_pack",
        type=str,
        help="Путь к ресурспаку OptiFine"
    )
    
    parser.add_argument(
        "-o", "--output",
        type=str,
        default="./converted_output",
        help="Директория вывода (по умолчанию: ./converted_output)"
    )
    
    parser.add_argument(
        "-n", "--namespace",
        type=str,
        default="converted_pack",
        help="Namespace для ItemsAdder (по умолчанию: converted_pack)"
    )
    
    parser.add_argument(
        "--assign-cmd",
        action="store_true",
        help="Автогенерировать custom_model_data для предметов"
    )
    
    parser.add_argument(
        "--no-interactive",
        action="store_true",
        help="Пропустить интерактивный редактор"
    )
    
    parser.add_argument(
        "--log-file",
        type=str,
        default="conversion.log",
        help="Путь к файлу лога (по умолчанию: conversion.log)"
    )
    
    args = parser.parse_args()
    
    # Настройка логирования
    logger = setup_logging(args.log_file)
    
    # Проверка пути
    source_path = Path(args.source_pack).resolve()
    if not source_path.exists():
        logger.error(f"Путь не найден: {source_path}")
        sys.exit(1)
        
    output_path = Path(args.output).resolve()
    
    # Создание конвертера
    converter = OptiFineConverter(
        source_pack_path=source_path,
        output_dir=output_path,
        namespace=args.namespace,
        assign_cmd=args.assign_cmd,
        interactive=not args.no_interactive
    )
    
    # Запуск конвертации
    success = converter.convert()
    
    # Вывод сводки
    converter.print_summary()
    
    if success:
        logger.info("Конвертация завершена успешно!")
        sys.exit(0)
    else:
        logger.error("Конвертация завершилась с ошибками!")
        sys.exit(1)


if __name__ == "__main__":
    main()
