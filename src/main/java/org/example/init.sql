CREATE USER 'user'@'localhost' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON task_manager.* TO 'user '@'localhost';
FLUSH PRIVILEGES;