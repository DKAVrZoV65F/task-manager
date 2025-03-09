package org.example;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class Main {
    private Map<String, DefaultListModel<String>> taskLists = new HashMap<>();
    private Map<String, JList<String>> listComponents = new HashMap<>();

    private JTextArea taskDetailsArea = new JTextArea(5, 30);

    private Connection connection;
    private String currentUser;
    private JFrame frame;

    static String urlDB = "jdbc:mysql://localhost:3306/task_manager";
    static String userDB = "user";
    static String passwordDB = "password";

    public Main(String user) {
        this.currentUser = user;
        connectToDB();

        frame = new JFrame("Канбан-доска - " + user);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 500);
        frame.setLayout(new BorderLayout());

        JPanel boardPanel = new JPanel(new GridLayout(1, 3));
        String[] statuses = {"TO_DO", "IN_PROGRESS", "DONE"};

        for (String status : statuses) {
            // Создаём модель и компонент списка для каждого статуса
            DefaultListModel<String> model = new DefaultListModel<>();
            taskLists.put(status, model);
            JList<String> list = new JList<>(model);
            list.setDragEnabled(true);
            list.setDropMode(DropMode.INSERT);
            list.setTransferHandler(new TaskTransferHandler(status));
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.addListSelectionListener(this::taskSelected);
            listComponents.put(status, list);

            // Загружаем задачи из БД для статуса
            loadTasks(status);

            JPanel columnPanel = new JPanel(new BorderLayout());
            JLabel label = new JLabel(getStatusLabel(status), SwingConstants.CENTER);
            label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            columnPanel.add(label, BorderLayout.NORTH);
            columnPanel.add(new JScrollPane(list), BorderLayout.CENTER);

            boardPanel.add(columnPanel);
        }

        JPanel buttons = getButtonsPanel();

        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBorder(BorderFactory.createTitledBorder("Детали задачи"));
        detailsPanel.add(new JScrollPane(taskDetailsArea), BorderLayout.CENTER);

        frame.add(boardPanel, BorderLayout.CENTER);
        frame.add(buttons, BorderLayout.SOUTH);
        frame.add(detailsPanel, BorderLayout.EAST);

        frame.setVisible(true);
    }

    private String getStatusLabel(String status) {
        return switch (status) {
            case "TO_DO" -> "Сделать";
            case "IN_PROGRESS" -> "В процессе";
            case "DONE" -> "Завершён";
            default -> status;
        };
    }

    private JPanel getButtonsPanel() {
        JPanel buttons = new JPanel();
        JButton newButton = new JButton("Создать");
        JButton saveButton = new JButton("Сохранить");
        JButton deleteButton = new JButton("Удалить");
        JButton statusButton = new JButton("Изменить статус");
        JButton commentButton = new JButton("Добавить комментарий");

        newButton.addActionListener(_ -> createTask());
        saveButton.addActionListener(_ -> saveTask());
        deleteButton.addActionListener(_ -> deleteTask());
        statusButton.addActionListener(_ -> changeStatus());
        commentButton.addActionListener(_ -> addComment());

        buttons.add(newButton);
        buttons.add(saveButton);
        buttons.add(deleteButton);
        buttons.add(statusButton);
        buttons.add(commentButton);
        return buttons;
    }

    private void connectToDB() {
        try {
            connection = DriverManager.getConnection(urlDB, userDB, passwordDB);
//            System.out.println("✅ Успешное подключение к MySQL");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Ошибка подключения к БД");
        }

        try {
            Statement statement = connection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS users " +
                    "(" +
                    "username TEXT PRIMARY KEY, " +
                    "password TEXT" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS tasks " +
                    "(" +
                    "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
                    "user TEXT, " +
                    "title TEXT, " +
                    "description TEXT, " +
                    "status TEXT, " +
                    "comment TEXT" +
                    ")");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Ошибка создание таблицы в БД");
        }
    }

    private void createTask() {
        String name = JOptionPane.showInputDialog("Название задачи:");
        if (name != null && !name.isBlank()) {
            String description = JOptionPane.showInputDialog("Описание задачи:");
            if (description == null) description = "";

            try {
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO tasks (user, title, status, description) VALUES (?, ?, ?, ?)"
                );
                statement.setString(1, currentUser);
                statement.setString(2, name);
                statement.setString(3, "TO_DO");
                statement.setString(4, description);
                statement.executeUpdate();

                reloadTasks();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveTask() {
        for (JList<String> list : listComponents.values()) {
            String selectedTask = list.getSelectedValue();
            if (selectedTask != null) {
                try {
                    PreparedStatement statement = connection.prepareStatement(
                            "UPDATE tasks SET comment = ? WHERE user = ? AND title = ?");
                    statement.setString(1, taskDetailsArea.getText());
                    statement.setString(2, currentUser);
                    statement.setString(3, selectedTask);
                    statement.executeUpdate();
                    JOptionPane.showMessageDialog(frame, "Комментарий сохранён");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return;
            }
        }
        JOptionPane.showMessageDialog(frame, "Выберите задачу для сохранения комментария");
    }

    private void deleteTask() {
        for (Map.Entry<String, JList<String>> entry : listComponents.entrySet()) {
            String selectedTask = entry.getValue().getSelectedValue();
            if (selectedTask != null) {
                try {
                    PreparedStatement statement = connection.prepareStatement(
                            "DELETE FROM tasks WHERE user = ? AND title = ?"
                    );
                    statement.setString(1, currentUser);
                    statement.setString(2, selectedTask);
                    statement.executeUpdate();

                    taskLists.get(entry.getKey()).removeElement(selectedTask);
                    reloadTasks();
                    JOptionPane.showMessageDialog(frame, "Задача удалена");
                    return;
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        JOptionPane.showMessageDialog(frame, "Выберите задачу для удаления");
    }

    private void changeStatus() {
        for (Map.Entry<String, JList<String>> entry : listComponents.entrySet()) {
            String currentStatus = entry.getKey();
            String selectedTask = entry.getValue().getSelectedValue();
            if (selectedTask != null) {
                String[] statuses = {"TO_DO", "IN_PROGRESS", "DONE"};
                String newStatus = (String) JOptionPane.showInputDialog(
                        frame, "Выберите новый статус:", "Изменить статус",
                        JOptionPane.QUESTION_MESSAGE, null, statuses, currentStatus
                );

                if (newStatus != null && !newStatus.equals(currentStatus)) {
                    updateTaskStatus(selectedTask, newStatus);
                    reloadTasks();
                    JOptionPane.showMessageDialog(frame, "Статус задачи изменён");
                }
                return;
            }
        }
        JOptionPane.showMessageDialog(frame, "Выберите задачу для изменения статуса");
    }

    private void addComment() {
        for (JList<String> list : listComponents.values()) {
            String selectedTask = list.getSelectedValue();
            if (selectedTask != null) {
                String comment = JOptionPane.showInputDialog("Введите комментарий:");
                if (comment != null) {
                    try {
                        PreparedStatement statement = connection.prepareStatement(
                                "UPDATE tasks SET comment = ? WHERE user = ? AND title = ?"
                        );
                        statement.setString(1, comment);
                        statement.setString(2, currentUser);
                        statement.setString(3, selectedTask);
                        statement.executeUpdate();

                        reloadTasks();
                        JOptionPane.showMessageDialog(frame, "Комментарий добавлен");
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                return;
            }
        }
        JOptionPane.showMessageDialog(frame, "Выберите задачу для добавления комментария");
    }

    private void taskSelected(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            JList<String> source = (JList<String>) e.getSource();
            String name = source.getSelectedValue();
            if (name != null) {
                try {
                    PreparedStatement statement = connection.prepareStatement(
                            "SELECT status, IFNULL(comment, '') as " +
                                    "comment FROM tasks WHERE user = ? AND title = ?"
                    );
                    statement.setString(1, currentUser);
                    statement.setString(2, name);
                    ResultSet resultSet = statement.executeQuery();
                    if (resultSet.next()) {
                        String status = resultSet.getString("status");
                        String comment = resultSet.getString("comment");
                        if (comment == null) {
                            comment = "";
                        }
                        taskDetailsArea.setText("Статус: " + getStatusLabel(status) +
                                "\nКомментарий: " + comment);
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void loadTasks(String status) {
        DefaultListModel<String> model = taskLists.get(status);
        model.clear();
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT title FROM tasks WHERE user = ? AND status = ?"
            );
            statement.setString(1, currentUser);
            statement.setString(2, status);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                model.addElement(resultSet.getString("title"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Обработчик drag'n'drop для смены статуса задачи при перемещении между колонками
    private class TaskTransferHandler extends TransferHandler {
        private String status;

        public TaskTransferHandler(String status) {
            this.status = status;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            JList<?> list = (JList<?>) c;
            Object value = list.getSelectedValue();
            if (value != null) {
                return new StringSelection(value.toString());
            }
            return null;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) return false;

            try {
                Transferable t = support.getTransferable();
                String taskName = (String) t.getTransferData(DataFlavor.stringFlavor);
                updateTaskStatus(taskName, status);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    // Обновление статуса задачи в базе данных
    private void updateTaskStatus(String taskName, String newStatus) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "UPDATE tasks SET status = ? WHERE user = ? AND title = ?"
            );
            statement.setString(1, newStatus);
            statement.setString(2, currentUser);
            statement.setString(3, taskName);
            statement.executeUpdate();
            reloadTasks();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Перезагружаем задачи для всех статусов
    private void reloadTasks() {
        for (String status : taskLists.keySet()) {
            taskLists.get(status).clear();
            loadTasks(status);
        }
    }

    public static void main(String[] args) {
        connectToDatabase();
        SwingUtilities.invokeLater(Main::showLoginScreen);
    }

    private static void showLoginScreen() {
        JFrame loginFrame = new JFrame("Авторизация");
        loginFrame.setSize(300, 200);
        loginFrame.setLayout(new GridLayout(3, 2));

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JButton loginButton = new JButton("Войти");
        JButton registerButton = new JButton("Регистрация");

        loginButton.addActionListener(_ -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            if (authenticate(username, password)) {
                loginFrame.dispose();
                new Main(username);
            } else {
                JOptionPane.showMessageDialog(loginFrame, "Неверный логин или пароль");
            }
        });

        registerButton.addActionListener(_ -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            if (register(username, password)) {
                JOptionPane.showMessageDialog(loginFrame, "Регистрация успешна");
            } else {
                JOptionPane.showMessageDialog(loginFrame, "Пользователь уже существует");
            }
        });

        loginFrame.add(new JLabel("Логин:"));
        loginFrame.add(usernameField);
        loginFrame.add(new JLabel("Пароль:"));
        loginFrame.add(passwordField);
        loginFrame.add(loginButton);
        loginFrame.add(registerButton);

        loginFrame.setVisible(true);
    }

    private static boolean authenticate(String username, String password) {
        try (Connection conn = DriverManager.getConnection(urlDB, userDB, passwordDB)) {
            PreparedStatement statement = conn.prepareStatement(
                    "SELECT * FROM users WHERE username = ? AND password = ?"
            );
            statement.setString(1, username);
            statement.setString(2, password);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean register(String username, String password) {
        try (Connection conn = DriverManager.getConnection(urlDB, userDB, passwordDB)) {
            PreparedStatement statement = conn.prepareStatement(
                    "INSERT INTO users (username, password) VALUES (?, ?)");
            statement.setString(1, username);
            statement.setString(2, password);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private static void connectToDatabase() {

        try (Connection _ = DriverManager.getConnection(urlDB, userDB, passwordDB)) {
            System.out.println("✅ Успешное подключение к MySQL");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Ошибка подключения к БД");
        }

        try (Connection connection = DriverManager.getConnection(urlDB, userDB, passwordDB)) {
            Statement statement = connection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS users " +
                    "(" +
                    "username VARCHAR(255) PRIMARY KEY, " +
                    "password VARCHAR(255)" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS tasks " +
                    "(" +
                    "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
                    "user VARCHAR(255), " +
                    "title VARCHAR(255), " +
                    "description VARCHAR(255), " +
                    "status VARCHAR(255), " +
                    "comment VARCHAR(255)" +
                    ")");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Ошибка создание таблицы в БД");
        }
    }
}