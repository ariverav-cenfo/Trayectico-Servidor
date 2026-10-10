# Trayectico-Servidor

########
Variables .env

El docker compose necesita un archivo .env con las variables que pueda ocupar el compose en el mismo folder:
Docker 
  --->docker-compose.yml
  --->.env

Actualmente el compose necesita las siguientes variables para la BD
DB_USER=USUARIOEJEMPLO
DB_PASSWORD=PASSWORDEJEMPLO

*Para la contraseña en el docker, postgre puede tener problemas interpretando valores como / $ " 

Adicionalmente los environment variables de spring no estan configurados en un archivo .env, estan configurados directamente en el application properties: 
1- En intellij junto a la opcion de run seleccionar el drop down
2- En el drop down seleccionar "Edit configurations" 
3- En "Environment variables" seleccionar "Edit environment variables" 
4- Agregar variables, el nombre de la variable es igual que a como estan definidos en el application settings pero sin el $ 
5- Click OK
6- Click Apply y despues OK
