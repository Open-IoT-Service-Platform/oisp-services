FROM node:8-alpine

RUN npm install kafkajs validator ws keycloak-connect uuid winston
ADD streamer.js streamer.js
ADD config.js config.js
ADD keycloak.json keycloak.json

EXPOSE 9000

CMD ["node", "streamer.js"]