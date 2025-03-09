# TaskManager Kanban Board

TaskManager — это Java-приложение для управления задачами с использованием Канбан-доски и поддержкой drag-and-drop. Приложение взаимодействует с базой данных MySQL для хранения пользователей и задач.

## 🚀 Возможности
- Авторизация и регистрация пользователей.
- Создание, изменение и удаление задач.
- Перемещение задач между колонками TO_DO, IN_PROGRESS и DONE с помощью drag-and-drop.
- Добавление комментариев к задачам.
- Сохранение данных в MySQL.

## Требования
- Java 22
- MySQL 8+
- JDBC Driver for MySQL

## 🛠️ Установка и запуск

### 1. Настройка базы данных
Перед запуском необходимо настроить базу данных MySQL.

```sql
CREATE DATABASE task_manager;
USE task_manager;

CREATE TABLE users (
    username VARCHAR(255) PRIMARY KEY,
    password VARCHAR(255)
);

CREATE TABLE tasks (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    user VARCHAR(255),
    title VARCHAR(255),
    description TEXT,
    status VARCHAR(255),
    comment TEXT
);
```

Далее создайте пользователя и предоставьте ему права:

```sql
CREATE USER 'user'@'localhost' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON task_manager.* TO 'user'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Запуск приложения
1. Склонируйте репозиторий:
   ```sh
   git clone https://github.com/DKAVrZoV65F/task-manager.git
   cd task-manager/src/main/java/org/example/
   ```
2. Создайте базу данных `task_manager` и выполните скрипт `init.sql` в MySQL:
   ```sh
   mysql -u root -p < init.sql
   ```
3. Проект использует **Maven**. Для сборки и запуска выполните:
   ```sh
   mvn clean install
   java -jar Main.jar
   ```

## Использование
1. При запуске появится окно авторизации.
2. Можно зарегистрировать нового пользователя или войти под существующей учетной записью.
3. После входа открывается Канбан-доска с тремя колонками: "Сделать", "В процессе" и "Завершено".
4. Для добавления задачи нажмите "Создать", введите название и описание.
5. Можно менять статус задач путем перемещения между колонками.
6. Для редактирования или удаления задачи выберите её в списке и используйте соответствующие кнопки.
