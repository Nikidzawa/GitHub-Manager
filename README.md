# GitHub Manager
## Desktop версия
### Интерфейс

Приложение для персональных оповещений. Запускается в методе main() класса Starter.

Если gitHub api не буянит(а это происходит весьма часто), то у вас в панели задач должно было появится небольная иконка нашего менеджера, при нажатии на которую правой кнопкой мыши, у нас откроется небольшое меню:

![screen_menu.png](src%2Fmain%2Fresources%2Fscreen_menu.png)

Здесь есть всё что вам может пригодится.
Начнём с самого верха, первая кнопка - это имя нашего аккаунта, она перенаправит на вашу страницу на гитхабе.

Далее - нотификации, тут всё понятно.

Потом у нас идёт список репозиториев. При наведении курсора, вам выдаст списки наших репозиториев, при наведении на какой-нибудь репозиторий, нам выдаст информацию которую мы хотим получить. Например, при наведении на пулл реквесты, нам выдаст  список активных пулл реквестов, ну и далее по скрину. Вся информация обновляется динамично, то есть вам не придётся перезапускать приложение чтобы обновились изменения. В целом, сама суть приложения заключается в том, чтобы постоянно быть уведомлённым об активностях в репозитории, чтобы оперативно увидеть и/или отреагировать на изменения.

![screen_repo.png](src%2Fmain%2Fresources%2Fscreen_repo.png)

И последняя панель - это настройки, в которых мы можем менять язык(Изменяется локализация как самого интерфейса, так и уведомлений), а так же настраивать уведомления которые мы не хотим получать, например, мы можем отключить оповещения о коммитах

![screen_sett.png](src%2Fmain%2Fresources%2Fscreen_sett.png)

### Уведомления
Ну а теперь к самой сути приложения - это уведомления. Давайте для примера представим, что нам поставили лайк в репозитории. Если в настройках у нас включены подобные уведомления, то далее, в зависимости от выбранного языка, мы получим примерно следующее уведомление

![screen_message.png](src%2Fmain%2Fresources%2Fscreen_message.png)

## GitHub Manager (Telegram Bot)
Бот выполянет почти те же функции что и десктоп версия, но может использоваться в том числе на телефонах и имеет более приятный интерфейс. Из функционала я убрал вывод всех репозиториев, ибо это выглядело бы несуразно. Вместо этого, я добавил к уведомлениям все необходимые ссылки. Выбранные настройки сохраняются даже если перезапустить сессию. В общем, всё сделано максимально удобно

К боту подключена база данных. Для удобства, токены будут хранится в ней, естественно в зашифрованном виде

![screen_tg_bot_first.png](src%2Fmain%2Fresources%2Fscreen_tg_bot_first.png)

Примеры уведомлений

![screen_tg_bot_second.png](src%2Fmain%2Fresources%2Fscreen_tg_bot_second.png)

![screen_tg_bot_third.png](src%2Fmain%2Fresources%2Fscreen_tg_bot_third.png)

Ссылка на бота: https://t.me/Git_Manager_bot