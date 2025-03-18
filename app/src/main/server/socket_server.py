import os

from flask import Flask, request, jsonify, url_for, send_from_directory
import pymysql
import bcrypt
from flask_socketio import SocketIO, emit

app = Flask(__name__)

socketio = SocketIO(app, cors_allowed_origins="*")  # пока что не реализованно

UPLOAD_FOLDER = 'uploads'
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)


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
            sql = "SELECT id, email, password, name, permission FROM users WHERE email=%s"
            cursor.execute(sql, email)
            result = cursor.fetchone()
        conn.close()

        if result:
            stored_hash = result['password']
            if bcrypt.checkpw(password.encode('utf-8'), stored_hash.encode('utf-8')):
                return jsonify({
                    'success': True,
                    'userId': result['id'],
                    'name': result['name'],
                    'permission': result['permission']
                })
            else:
                # Если пароль неверный
                return jsonify({'success': False, 'message': 'Неверный email или пароль'})
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


@app.route('/addrecipes', methods=['POST'])
def create_recipe():
    title = request.form.get('title')
    ingredients = request.form.get('ingredients')
    instructions = request.form.get('instructions')
    creatId = request.form.get('userId')

    # Проверяем, что все необходимые поля заполнены
    if not (title and ingredients and instructions):
        return jsonify({'success': False, 'message': 'Все поля (title, ingredients, instructions) обязательны'}), 400

    photo_url = None
   # if 'photo' in request.files:
    #    photo_file = request.files['photo']
        #if photo_file and allowed_file(photo_file.filename):
         #   filename = secure_filename(photo_file.filename)
          #  file_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
           # photo_file.save(file_path)
            # Генерируем URL для доступа к файлу
            #photo_url = url_for('uploaded_file', filename=filename, _external=True)
        #else:
         #   return jsonify({'success': False, 'message': 'Неверный формат файла'}), 400

    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            sql_insert = "INSERT INTO recipes (title, ingredients, instructions, user_id, photo) VALUES (%s, %s, %s, %s, %s)"
            cursor.execute(sql_insert, (title, ingredients, instructions, creatId, photo_url))
        conn.commit()
        conn.close()
        # Отправка уведомления пользователям(не реализованно)
        # socketio.emit("new_recipe", 1)
        return jsonify({'success': True, 'message': 'Рецепт успешно создан'})
    except Exception as e:
        print("Ошибка при создании рецепта:", e)
        return jsonify({'success': False, 'error': str(e)}), 500


@app.route('/deliterecipe', methods=['POST'])
def delete_recipe():  # Исправлено название (было delite_recipe)
    data = request.get_json()
    id = data.get('id')
    user_id = data.get('userId')
    permission = data.get('permission')

    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            if permission == 2:  # Убраны ненужные скобки
                # Блок if (4 пробела)
                sql_delete = "DELETE FROM recipes WHERE id = %s;"
                cursor.execute(sql_delete, (id,))  # Кортеж с одним элементом
            else:
                # Блок else (4 пробела)
                sql_delete = "DELETE FROM recipes WHERE id = %s AND user_id = %s;"  # Исправлено количество параметров
                cursor.execute(sql_delete, (id, user_id))  # Только два параметра

            conn.commit()

        return jsonify({'success': True, 'message': 'Рецепт удален'})

    except Exception as e:
        print("Ошибка при удалении рецепта:", e)  # Исправлено сообщение об ошибке
        return jsonify({'success': False, 'error': str(e)}), 500

    finally:
        if 'conn' in locals() and conn is not None:  # Безопасное закрытие соединения
            conn.close()

    # Эндпоинт для раздачи загруженных файлов (фото рецептов)


@app.route('/uploads/<filename>')
def uploaded_file(filename):
    return send_from_directory(app.config['UPLOAD_FOLDER'], filename)


# Дополним эндпоинт для получения рецептов с более подробным логированием
@app.route('/recipes', methods=['GET'])
def get_recipes():
    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            # Используем alias "userId" для поля user_id
            sql = "SELECT id, title, ingredients, instructions, created_at, user_id as userId, photo FROM recipes ORDER BY id DESC"
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
 #Добавления лайка   
@app.route('/like', methods=['POST'])
def like_recipe():
    try:
        data = request.get_json()
        user_id = data.get('userId')
        recipe_id = data.get('recipeId')
        
        if not user_id or not recipe_id:
            return jsonify({'success': False, 'message': 'userId и recipeId обязательны'}), 400

        conn = get_db_connection()
        with conn.cursor() as cursor:
            # Проверяем, если лайк уже существует, можно вернуть соответствующее сообщение
            check_sql = "SELECT * FROM user_likes WHERE user_id = %s AND recipe_id = %s"
            cursor.execute(check_sql, (user_id, recipe_id))
            if cursor.fetchone() is not None:
                conn.close()
                return jsonify({'success': False, 'message': 'Рецепт уже лайкнут'}), 409
            
            # Если лайка еще нет, вставляем новую запись
            insert_sql = "INSERT INTO user_likes (user_id, recipe_id) VALUES (%s, %s)"
            cursor.execute(insert_sql, (user_id, recipe_id))
        conn.commit()
        conn.close()
        return jsonify({'success': True, 'message': 'Лайк добавлен'})
    except Exception as e:
        print(f"Ошибка при лайке рецепта: {str(e)}")
        return jsonify({'success': False, 'message': f'Server error: {str(e)}'}), 500

    
    
    #Для получения понравившегося рецепта 
@app.route('/likedrecipes', methods=['GET'])
def get_liked_recipes():
    user_id = request.args.get('userId')
    if not user_id:
        return jsonify({'success': False, 'message': 'userId не указан'}), 400
    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            # Выбираем рецепты, присоединив таблицу user_likes по recipe_id
            sql = """
                SELECT r.id, r.title, r.ingredients, r.instructions, r.created_at, r.photo, r.user_id as userId
                FROM recipes r 
                JOIN user_likes ul ON r.id = ul.recipe_id
                WHERE ul.user_id = %s
                ORDER BY r.created_at DESC
            """
            cursor.execute(sql, (user_id,))
            recipes = cursor.fetchall()
        conn.close()

        # Форматируем поле created_at, если оно есть
        for recipe in recipes:
            if recipe.get('created_at'):
                recipe['created_at'] = recipe['created_at'].strftime('%Y-%m-%d %H:%M:%S')
        return jsonify({'success': True, 'recipes': recipes, 'count': len(recipes)})
    except Exception as e:
        print(f"Ошибка при получении лайкнутых рецептов: {str(e)}")
        return jsonify({'success': False, 'message': f'Ошибка сервера: {str(e)}'}), 500



# Запуск сервера
if __name__ == '__main__':
    # Запускаем на всех интерфейсах (0.0.0.0) и на порту 19029
    app.run(host='0.0.0.0', port=19029, debug=True)