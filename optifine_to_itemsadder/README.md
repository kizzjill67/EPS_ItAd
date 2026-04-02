# OptiFine CIT to ItemsAdder Converter

CLI-утилита для конвертации ресурспаков OptiFine CIT в аддоны ItemsAdder для Minecraft 1.21.5+ (ItemsAdder 3.6+)

## 📋 Описание

Программа автоматически сканирует `.properties` файлы в директории `assets/minecraft/optifine/cit/`, парсит их, извлекает текстуры и модели, и генерирует полностью рабочий аддон ItemsAdder с YAML конфигурацией.

## ✨ Возможности

- **Автоматический парсинг** `.properties` файлов OptiFine CIT
- **Извлечение текстур и моделей** из исходного ресурспака
- **Генерация YAML конфигов** ItemsAdder с NBT-матчингом
- **Поддержка брони** с текстурами слоёв (layer_1, layer_2)
- **Интерактивный CLI редактор** для пост-обработки предметов
- **Логирование** в консоль и файл
- **Graceful fallback** при отсутствии ресурсов

## 🛠️ Требования

- Python 3.10+
- Библиотеки:
  - `pyyaml>=6.0`
  - `rich>=13.0.0`

## 📦 Установка

```bash
# Клонируйте или скачайте файлы в директорию
cd optifine_to_itemsadder

# Установите зависимости
pip install -r requirements.txt
```

## 🚀 Использование

### Базовое использование

```bash
python converter.py /path/to/optifine_resourcepack
```

### Параметры командной строки

```
usage: converter.py [-h] [-o OUTPUT] [-n NAMESPACE] [--assign-cmd] [--no-interactive] [--log-file LOG_FILE] source_pack

OptiFine CIT to ItemsAdder Converter

 positional arguments:
  source_pack           Путь к ресурспаку OptiFine

 optional arguments:
  -h, --help            show this help message and exit
  -o OUTPUT, --output OUTPUT
                        Директория вывода (по умолчанию: ./converted_output)
  -n NAMESPACE, --namespace NAMESPACE
                        Namespace для ItemsAdder (по умолчанию: converted_pack)
  --assign-cmd          Автогенерировать custom_model_data для предметов
  --no-interactive      Пропустить интерактивный редактор
  --log-file LOG_FILE   Путь к файлу лога (по умолчанию: conversion.log)
```

### Примеры

```bash
# Конвертация с базовыми настройками
python converter.py C:\Users\Name\Desktop\my_optifine_pack

# Конвертация с кастомным namespace
python converter.py /home/user/packs/my_pack -n my_custom_pack

# Конвертация с автогенерацией CMD и без интерактивного редактора
python converter.py ./resource_pack --assign-cmd --no-interactive

# Конвертация с кастомным выводом
python converter.py ./pack -o ./itemsadder_output -n my_items
```

## 📁 Структура выходных данных

После конвертации создаётся следующая структура:

```
converted_output/
└── items_packs/
    └── <namespace>/
        ├── config.yml              # Основная конфигурация ItemsAdder
        ├── conversion_metadata.json # Метаданные конвертации
        └── assets/
            └── <namespace>/
                ├── textures/
                │   └── item/       # Текстуры предметов
                │   └── entity/
                │       └── armor/  # Текстуры брони
                └── models/
                    └── item/       # JSON модели
```

### Пример config.yml

```yaml
info:
  namespace: "converted_pack"
  author: "OptiFine Converter"
  version: "1.0.0"
  description: "Converted from OptiFine CIT pack"
items:
  legendary_sword:
    display_name: "&6Legendary Sword"
    resource_pack:
      model_id: "converted_pack:legendary_sword"
      generate: false
    properties:
      nbt:
        display:
          Name: '{"text":"Legendary Sword","italic":false,"color":"gold"}'
```

## 🔧 Интерактивный редактор

После конвертации автоматически запускается интерактивный CLI редактор, который позволяет:

1. **Просмотреть таблицу** всех сгенерированных предметов
2. **Выбрать предмет** по ID для редактирования
3. **Изменить параметры**:
   - `display_name` — отображаемое имя
   - `custom_model_data` — CMD значение
   - `NBT свойства` — матчинг по NBT
   - `durability` — прочность предмета
   - `enchants` — зачарования
   - `armor` — параметры брони (слоты, текстуры, 3D модели)
   - `resource_pack` — настройки ресурспака

### Команды редактора

- Введите **ID предмета** для редактирования
- `all` — массовое редактирование всех предметов
- `skip` — пропустить редактирование
- `save & exit` — сохранить изменения и выйти
- `exit without save` — выйти без сохранения

## ⚠️ Важные замечания

### Regex паттерны
Если `.properties` файл содержит regex-паттерны в имени (например, `.*Sword.*`), конвертер сохранит оригинальный NBT-матчинг, но выведет предупреждение. ItemsAdder поддерживает regex через `lore` и `name` с использованием `regex: true`.

### JSON компоненты
JSON-компоненты имени сохраняются в оригинальном виде в NBT-матчинге.

### Отсутствующие ресурсы
Если текстура или модель не найдены, конвертер продолжит работу и запишет предупреждения в лог.

### custom_model_data
По умолчанию **не генерируется**. Используйте флаг `--assign-cmd` для автогенерации CMD значений.

## 🐛 Решение проблем

### Ошибка "Директория CIT не найдена"
Убедитесь, что ресурспак содержит структуру:
```
pack/
├── pack.mcmeta
└── assets/
    └── minecraft/
        └── optifine/
            └── cit/
                └── *.properties
```

### Ошибка кодировки
Конвертер автоматически пытается UTF-8 и Latin-1 кодировки. Если проблема сохраняется, проверьте файл лога.

### Предметы не появляются в игре
1. Проверьте, что ItemsAdder установлен
2. Убедитесь, что аддон в правильной директории (`plugins/ItemsAdder/items_packs/`)
3. Выполните `/iazip` и `/iareload`
4. Проверьте логи ItemsAdder на ошибки

## 📝 Логи

Логирование ведётся в:
- **Консоль** — основные события и прогресс
- **Файл** `conversion.log` — детальная отладочная информация

## 📄 Лицензия

MIT License — свободное использование и модификация.

## 🤝 Поддержка

При возникновении проблем:
1. Проверьте файл `conversion.log`
2. Убедитесь, что все зависимости установлены
3. Проверьте структуру исходного ресурспака

---

**Совместимость:** Minecraft 1.21.5+, ItemsAdder 3.6+
