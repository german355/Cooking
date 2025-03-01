# Импорт необходимых модулей:
# Flask - для создания веб-сервера;
# request - для работы с входящими HTTP-запросами;
# jsonify - для формирования JSON-ответа.
from flask import Flask, request, jsonify
import cryptography

# Импортируем модуль pymysql для подключения и работы с базой данных MySQL.
import pymysql

# Создаем экземпляр приложения Flask.
app = Flask(__name__)

# Настраиваем подключение к базе данных MySQL.
# Пожалуйста, замените 'your_username', 'your_password' и 'your_database' на актуальные данные.
connection = pymysql.connect(
    host='cdb.veroid.net',  # Адрес сервера базы данных
    user='u23298_2xL9nWI7Ta',  # Имя пользователя базы данных
    password='kohpK.S8=2D12hELw!=boPNC',  # Пароль пользователя
    database='s23298_Samsung_progect',  # Имя базы данных
    charset='utf8mb4',  # Кодировка
    cursorclass=pymysql.cursors.DictCursor  # Результаты запросов будут возвращаться в виде словарей
)

# Обработчик для главной страницы
@app.route('/')
def index():
    return "Добро пожаловать на сервер!"

# Определяем маршрут '/login' для обработки POST-запросов.
@app.route('/login', methods=['POST'])
def login():
    # Получаем данные из JSON-тела запроса.
    data = request.get_json()
    email = data.get('email')  # Извлекаем email
    password = data.get('password')  # Извлекаем password

    try:
        # Открываем курсор для выполнения SQL-запроса.
        with connection.cursor() as cursor:
            # SQL-запрос для поиска пользователя с заданными email и password.
            # Используем параметризованный запрос для защиты от SQL-инъекций.
            sql = "SELECT * FROM users WHERE email=%s AND password=%s"
            cursor.execute(sql, (email, password))
            # Получаем первую запись из результата запроса.
            result = cursor.fetchone()

            # Если запись найдена, возвращаем JSON с ключом 'success' равным True.
            if result:
                return jsonify({'success': True})
            else:
                # Если записи нет, возвращаем JSON с 'success' равным False.
                return jsonify({'success': False})
    except Exception as e:
        # В случае ошибки выводим сообщение в консоль и возвращаем ошибку 500 с описанием.
        print("Ошибка запроса к базе данных:", e)
        return jsonify({'success': False, 'error': str(e)}), 500


# Основной блок запуска сервера.
# Сервер будет работать на порту 3000. Режим debug=True используется только в процессе разработки.
if __name__ == '__main__':
    app.run(port=3000, debug=True)
