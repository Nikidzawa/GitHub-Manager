# GitHub Manager
## Telegram Bot
### Интерфейс
Бот выполняет почти те же функции, что и десктоп версия, но может использоваться, в том числе на телефонах и имеет более приятный интерфейс. Давайте покажу ваш предполагаемый порядок действий

![start.png](src%2Fmain%2Fresources%2Fscreenshots%2Ftelegram%2Fstart.png)

Если это наш не первый запуск, то будет возможность взять уже ранее введённый токен из базы данных. Можете не переживать за сохранность токена, ибо всё шифруется.
Как только мы ввели токен, у нас появляется меню. Здесь находится наше фото, а так же написано какой аккаунт открыт. В самом меню мы можем:
1. Перейти в наш аккаунт
2. Перейти к нотификациям на GitHub
3. Мы можем посмотреть все репозитории или скрыть их, если они открыты
4. Можем включать или отключать сессию (бот прекращает все обращения к нашему GitHub)
5. можем открыть настройки бота

При нажатии на репозитории, у нас обновляется наше меню

![repos.png](src%2Fmain%2Fresources%2Fscreenshots%2Ftelegram%2Frepos.png)

Здесь нам показывает все наши репозитории, а так же все необходимые гиперссылки. Нашу сессию так же можно остановить. Мы узнаем это, если у нас обновится статус на такой

![status.png](src%2Fmain%2Fresources%2Fscreenshots%2Ftelegram%2Fstatus.png)

Давайте теперь откроем настройки

![settings.png](src%2Fmain%2Fresources%2Fscreenshots%2Ftelegram%2Fsettings.png)

Тут мы имеем возможность изменять язык, выбирать какие уведомления мы хотим получать, а так же задавать время жизни сообщений (по умолчанию отключены)

### Уведомления
Вот так выглядят уведомления. Например, в случае совершения коммита, мы получим уведомление, ссылку на сам коммит и репозиторий к которому он относится (пулл реквесты делать не стал, но в случае с ними, функционал тот же)

![notifications.png](src%2Fmain%2Fresources%2Fscreenshots%2Ftelegram%2Fnotifications.png)

ну и последняя команда, которая выдаёт нам небольшую информацию

![info.png](src%2Fmain%2Fresources%2Fscreenshots%2Ftelegram%2Finfo.png)

Ссылка на бота: https://t.me/Git_Manager_bot
## Desktop версия
### Интерфейс

Приложение для персональных оповещений. Запускается в методе main() класса Starter, который расположен в соответствующей папке Desktop.

Если gitHub api не буянит(а это происходит весьма часто), то у вас в панели задач должно было появится небольшая иконка нашего менеджера, при нажатии на которую правой кнопкой мыши, у нас откроется небольшое меню:

![screen_menu.png](src%2Fmain%2Fresources%2Fscreenshots%2Fdesktop%2Fscreen_menu.png)

Здесь есть всё что вам может пригодится.
Начнём с самого верха, первая кнопка - это имя нашего аккаунта, она перенаправит на вашу страницу на GitHub.

Далее - нотификации, тут всё понятно.

Потом у нас идёт список репозиториев. При наведении курсора, вам выдаст списки наших репозиториев, при наведении на какой-нибудь репозиторий, нам выдаст информацию которую мы хотим получить. Например, при наведении на пулл реквесты, нам выдаст  список активных пулл реквестов, ну и далее по скрину. Вся информация обновляется динамично, то есть вам не придётся перезапускать приложение чтобы обновились изменения. В целом, сама суть приложения заключается в том, чтобы постоянно быть уведомлённым об активностях в репозитории, чтобы оперативно увидеть и/или отреагировать на изменения.

![screen_repo.png](src%2Fmain%2Fresources%2Fscreenshots%2Fdesktop%2Fscreen_repo.png)

И последняя панель - это настройки, в которых мы можем менять язык(Изменяется локализация как самого интерфейса, так и уведомлений), а так же настраивать уведомления которые мы не хотим получать, например, мы можем отключить оповещения о коммитах

![screen_sett.png](src%2Fmain%2Fresources%2Fscreenshots%2Fdesktop%2Fscreen_sett.png)

### Уведомления
Ну а теперь к самой сути приложения - это уведомления. Давайте для примера представим, что нам поставили лайк в репозитории. Если в настройках у нас включены подобные уведомления, то далее, в зависимости от выбранного языка, мы получим примерно следующее уведомление

![screen_message.png](src%2Fmain%2Fresources%2Fscreenshots%2Fdesktop%2Fscreen_message.png)