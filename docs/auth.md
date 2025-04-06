# Система авторизации в приложении "Cooking"

## Обзор системы авторизации

Приложение "Cooking" использует Firebase Authentication для управления аутентификацией пользователей. Система авторизации обеспечивает следующие функции:

- Регистрация новых пользователей (email/пароль)
- Авторизация существующих пользователей (email/пароль)
- Авторизация через Google Sign-In
- Восстановление пароля
- Управление профилем пользователя
- Безопасный выход из аккаунта
- Удаление аккаунта

## Архитектура системы авторизации

### Компоненты

1. **FirebaseAuthManager** - центральный класс, управляющий взаимодействием с Firebase Authentication
2. **AuthViewModel** - ViewModel для экрана авторизации
3. **ProfileViewModel** - ViewModel для экрана профиля
4. **MainViewModel** - ViewModel для основной активности, отслеживает состояние авторизации
5. **AuthFragment** - UI-компонент для авторизации пользователя
6. **ProfileFragment** - UI-компонент для отображения и управления профилем
7. **RegisterActivity (Regist)** - Активность для регистрации нового пользователя

### Диаграмма взаимодействия компонентов

```
┌─────────────┐     ┌───────────────┐     ┌───────────────────┐
│ AuthFragment │◄───►│ AuthViewModel │◄───►│ FirebaseAuthManager│
└─────────────┘     └───────────────┘     └───────────────────┘
                          ▲                        ▲
                          │                        │
                          │                        │
┌─────────────┐     ┌───────────────┐             │
│ProfileFragment◄───►│ProfileViewModel◄─────────────┘
└─────────────┘     └───────────────┘
                          ▲
                          │
┌─────────────┐     ┌───────────────┐
│ MainActivity │◄───►│ MainViewModel │
└─────────────┘     └───────────────┘
```

## Процессы авторизации

### Регистрация нового пользователя

1. Пользователь вводит email, пароль и другие данные в RegisterActivity
2. AuthViewModel валидирует введенные данные
3. При успешной валидации вызывается FirebaseAuthManager.registerUser()
4. FirebaseAuthManager создает нового пользователя в Firebase
5. Результат регистрации передается обратно в ViewModel через callback
6. При успешной регистрации пользователь перенаправляется на экран входа

### Авторизация пользователя через Email/Password

1. Пользователь вводит email и пароль в AuthFragment
2. AuthViewModel валидирует введенные данные
3. При успешной валидации вызывается FirebaseAuthManager.signInWithEmailPassword()
4. Результат авторизации передается обратно в ViewModel через callback
5. При успешной авторизации:
   - Данные пользователя сохраняются в SharedPreferences
   - isAuthenticated устанавливается в true
   - MainViewModel получает уведомление о смене статуса авторизации
   - MainActivity переключает фрагмент с AuthFragment на ProfileFragment

### Авторизация через Google

1. Пользователь нажимает кнопку Google Sign-In в AuthFragment
2. AuthViewModel вызывает FirebaseAuthManager.signInWithGoogle()
3. Запускается интерфейс Google Sign-In через Intent
4. Результат авторизации обрабатывается в onActivityResult() и передается в AuthViewModel
5. При успешной авторизации последовательность действий аналогична авторизации через Email/Password

### Выход из аккаунта

1. Пользователь нажимает кнопку выхода в ProfileFragment
2. ProfileViewModel вызывает FirebaseAuthManager.signOut()
3. FirebaseAuthManager выполняет выход из Firebase
4. Данные пользователя очищаются из SharedPreferences
5. ProfileViewModel устанавливает isAuthenticated в false и вызывает операцию success
6. MainViewModel получает событие выхода и запускает переключение на AuthFragment

### Удаление аккаунта

1. Пользователь подтверждает удаление аккаунта в ProfileFragment
2. ProfileViewModel вызывает FirebaseAuthManager.deleteAccount()
3. FirebaseAuthManager выполняет переаутентификацию (если необходимо) и удаляет аккаунт
4. Данные пользователя очищаются из SharedPreferences
5. Процесс завершается аналогично выходу из аккаунта

## Хранение данных пользователя

1. **Временные данные сессии**:
   - Хранятся в ViewModel с использованием LiveData
   - Включают имя пользователя, email, статус авторизации

2. **Постоянные данные**:
   - Сохраняются в SharedPreferences через класс MySharedPreferences
   - Включают userID, имя пользователя, email, уровень доступа

## Безопасность

1. **Токены авторизации**:
   - Управляются Firebase SDK
   - Токены хранятся в защищенном хранилище Firebase

2. **Валидация данных**:
   - Проверка формата email
   - Проверка сложности пароля (минимум 6 символов)
   - Проверка соответствия полей при регистрации

3. **Обработка ошибок**:
   - Отображение информативных сообщений об ошибках
   - Логирование ошибок для отладки

## Код для реализации авторизации

### Пример авторизации через email/пароль:

```java
// В AuthViewModel
public void signInWithEmailPassword(String email, String password) {
    isLoading.setValue(true);
    
    authManager.signInWithEmailPassword(email, password, new FirebaseAuthManager.AuthCallback() {
        @Override
        public void onSuccess(FirebaseUser user) {
            isLoading.postValue(false);
            isAuthenticated.postValue(true);
            saveUserData(user);
        }
        
        @Override
        public void onError(String message) {
            isLoading.postValue(false);
            errorMessage.postValue(message);
        }
    });
}
```

### Пример валидации данных:

```java
// В AuthViewModel
public boolean validateEmail(String email) {
    isEmailValid = !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    return isEmailValid;
}

public boolean validatePassword(String password) {
    isPasswordValid = !TextUtils.isEmpty(password) && password.length() >= 6;
    return isPasswordValid;
}
```

### Пример выхода из аккаунта:

```java
// В ProfileViewModel
public void signOut() {
    try {
        authManager.signOut();
        preferences.clear();
        isAuthenticated.postValue(false);
        operationSuccess.postValue(true);
    } catch (Exception e) {
        errorMessage.postValue("Произошла ошибка при выходе");
        operationSuccess.postValue(false);
    }
}
```

## Рекомендации по использованию

1. **Для разработчиков**:
   - Используйте FirebaseAuthManager для всех операций аутентификации
   - Не дублируйте логику авторизации в разных частях приложения
   - Проверяйте состояние авторизации при старте приложения через MainViewModel

2. **Для тестирования**:
   - Создайте тестовых пользователей для отладки
   - Используйте режим отладки Firebase для мониторинга процессов авторизации
   - Проверяйте граничные случаи (неверный пароль, несуществующий пользователь, и т.д.) 