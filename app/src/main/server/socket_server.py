from flask import Flask, request, jsonify
import pymysql
import bcrypt

app = Flask(__name__)


def hash_password(plain_password):
    # Генерация соли и хэширование пароля
    salt = bcrypt.gensalt()
    hashed = bcrypt.hashpw(plain_password.encode('utf-8'), salt)
    return hashed

# Функция для создания нового подключения к базе данных
def get_db_connection():
    return pymysql.connect(
        host='cdb.veroid.net',  # Адрес сервера базы данных
        user='u23298_2xL9nWI7Ta',  # Имя пользователя базы данных
        password='kohpK.S8=2D12hELw!=boPNC',  # Пароль пользователя
        database='s23298_Samsung_progect',  # Имя базы данных
        charset='utf8mb4',  # Кодировка
        cursorclass=pymysql.cursors.DictCursor  # Результаты запросов возвращаются в виде словарей
    )


# Обработчик для главной страницы
@app.route('/')
def index():
    return "Добро пожаловать на сервер!"


# Эндпоинт для логина
@app.route('/login', methods=['POST'])
def login():
    try:
        data = request.get_json()
        if not data:
            return jsonify({'success': False, 'message': 'No JSON data provided'}), 400

        email = data.get('email')
        password = data.get('password')
        name = data.get('name')

        if not email or not password:
            return jsonify({'success': False, 'message': 'Email or password missing'}), 400

        conn = get_db_connection()
        with conn.cursor(pymysql.cursors.DictCursor) as cursor:  # Используем DictCursor
            # Выполняем запрос, включая id в результат
            sql = "SELECT id, email, password, name FROM users WHERE email=%s"
            cursor.execute(sql, email)
            result = cursor.fetchone()
        conn.close()

        if result:
            # Возвращаем успех вместе с id пользователя
            stored_hash = result['password']
            if bcrypt.checkpw(password.encode('utf-8'), stored_hash.encode('utf-8')):
                return jsonify({
                    'success': True,
                    'userId': result['id'],
                    'name': result['name']
                })
        else:
            return jsonify({'success': False, 'message': 'Неверный email или пароль'})
    except Exception as e:
        print(f"Login error: {str(e)}")
        return jsonify({'success': False, 'message': f'Server error: {str(e)}'}), 500


# Эндпоинт для регистрации новых пользователей
@app.route('/register', methods=['POST'])
def register():
    try:
        data = request.get_json()
        if not data:
            print("Registration failed: No JSON data")
            return jsonify({'success': False, 'message': 'No JSON data provided'}), 400

        email = data.get('email')
        password = data.get('password')
        name = data.get('name')

        print(f"Attempting to register user: {email}")

        if not email or not password:
            print(f"Registration failed: Missing credentials for {email}")
            return jsonify({'success': False, 'message': 'Email or password missing'}), 400

        # Проверка минимальной длины пароля
        if len(password) < 4:
            print(f"Registration failed: Password too short for {email}")
            return jsonify({'success': False, 'message': 'Пароль должен содержать минимум 4 символа'}), 400

        try:
            conn = get_db_connection()
            with conn.cursor() as cursor:
                # Проверяем, существует ли уже пользователь с таким email
                check_sql = "SELECT * FROM users WHERE email=%s"
                cursor.execute(check_sql, (email,))
                if cursor.fetchone() is not None:
                    conn.close()
                    print(f"Registration failed: User {email} already exists")
                    return jsonify({'success': False, 'message': 'Пользователь с таким email уже существует'}), 409


                insert_sql = "INSERT INTO users (email, password, name) VALUES (%s, %s, %s)"
                cursor.execute(insert_sql, (email, hash_password(password), name))
            conn.commit()
            conn.close()
            print(f"Registration successful for user: {email}")
            return jsonify({'success': True, 'message': 'Пользователь успешно зарегистрирован'})
        except Exception as e:
            print(f"Database error during registration for {email}: {str(e)}")
            return jsonify({'success': False, 'message': f'Database error: {str(e)}'}), 500
    except Exception as e:
        print(f"Registration error: {str(e)}")
        return jsonify({'success': False, 'message': f'Server error: {str(e)}'}), 500


# ---------------------------------------------------------------------
# Добавлены новые эндпоинты для работы с рецептами (общими для всех пользователей)
# Предполагается, что в базе данных существует таблица "recipes" со следующей структурой:
# CREATE TABLE recipes (
#     id INT AUTO_INCREMENT PRIMARY KEY,
#     title VARCHAR(255) NOT NULL,
#     ingredients TEXT NOT NULL,
#     instructions TEXT NOT NULL,
#     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
# );
#
# Эндпоинт POST /recipes - для создания нового рецепта.
# Эндпоинт GET /recipes - для получения всех рецептов.

# Эндпоинт для создания нового рецепта
@app.route('/addrecipes', methods=['POST'])
def create_recipe():
    data = request.get_json()
    title = data.get('title')
    ingredients = data.get('ingredients')
    instructions = data.get('instructions')
    creatId = data.get('userId')

    # Проверяем, что все необходимые поля заполнены
    if not (title and ingredients and instructions):
        return jsonify({'success': False, 'message': 'Все поля (title, ingredients, instructions) обязательны'}), 400

    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            sql_insert = "INSERT INTO recipes (title, ingredients, instructions, user_id) VALUES (%s, %s, %s, %s)"
            cursor.execute(sql_insert, (title, ingredients, instructions, creatId))
        conn.commit()
        conn.close()
        return jsonify({'success': True, 'message': 'Рецепт успешно создан'})
    except Exception as e:
        print("Ошибка при создании рецепта:", e)
        return jsonify({'success': False, 'error': str(e)}), 500



@app.route('/deliterecipe', methods=['POST'])
def delite_recipe():
    data = request.get_json()
    id = data.get('id')
    user_id = data.get('userId')
    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            sql_insert = "DELETE FROM recipes WHERE id = %s and user_id = %s;"
            cursor.execute(sql_insert, (id, user_id))
        conn.commit()
        conn.close()
        return jsonify({'success': True, 'message': 'Рецепт удален'})
    except Exception as e:
        print("Ошибка при создании рецепта:", e)
        return jsonify({'success': False, 'error': str(e)}), 500


# Дополним эндпоинт для получения рецептов с более подробным логированием
@app.route('/recipes', methods=['GET'])
def get_recipes():
    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            # Используем alias "userId" для поля user_id
            sql = "SELECT id, title, ingredients, instructions, created_at, user_id as userId FROM recipes ORDER BY id DESC"
            cursor.execute(sql)
            recipes = cursor.fetchall()
        conn.close()

        # Конвертируем datetime объекты в строки для JSON
        for recipe in recipes:
            if 'created_at' in recipe and recipe['created_at']:
                recipe['created_at'] = recipe['created_at'].strftime('%Y-%m-%d %H:%M:%S')

        response = {
            'success': True,
            'recipes': recipes,
            'count': len(recipes)
        }
        return jsonify(response)
    except Exception as e:
        print(f"Ошибка при получении рецептов: {e}")
        return jsonify({
            'success': False,
            'message': f'Ошибка сервера: {str(e)}',
            'recipes': []
        }), 500




# Запуск сервера
if __name__ == '__main__':
    # Запускаем на всех интерфейсах (0.0.0.0) и на порту 19029
    app.run(host='0.0.0.0', port=19029, debug=True)
