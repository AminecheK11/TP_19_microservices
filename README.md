
# TP19 — Orchestration de Microservices avec Spring Cloud

### Eureka Server | Spring Cloud Gateway | OpenFeign | Spring Boot

**Réalisé par :** Amine CHEKKOURI  
**Formation :** 5ème année — Informatique Industrielle et Réseaux (5IIR)  
**Établissement :** EMSI Marrakech  
**Année universitaire :** 2026–2027

---

## 1. Introduction

Ce travail pratique porte sur la conception et la mise en œuvre d'une architecture microservices basée sur **Spring Boot** et **Spring Cloud**.

L'objectif est de développer plusieurs services indépendants, de mettre en place un mécanisme de découverte des services avec **Netflix Eureka**, de centraliser les requêtes HTTP avec **Spring Cloud Gateway** et d'assurer la communication entre microservices grâce à **OpenFeign**.

L'architecture repose sur quatre applications :

- **Eureka Server :** serveur de découverte et d'enregistrement des microservices.
- **SERVICE-CLIENT :** microservice responsable de la gestion des clients.
- **Spring Cloud Gateway :** point d'entrée centralisé pour le routage des requêtes.
- **SERVICE-VOITURE :** microservice chargé de la gestion des voitures et de la communication avec SERVICE-CLIENT.

---

## 2. Technologies utilisées

| Technologie | Version / Utilisation |
|---|---|
| Java | 17 |
| Spring Boot | 3.5.16 |
| Spring Cloud | 2025.0.3 |
| Netflix Eureka | Découverte des microservices |
| Spring Cloud Gateway MVC | Routage des requêtes HTTP |
| Spring Cloud LoadBalancer | Résolution dynamique des services |
| OpenFeign | Communication interservices |
| Spring Data JPA | Persistance des données |
| H2 Database | Base de données en mémoire |
| Maven | Gestion des dépendances et compilation |
| IntelliJ IDEA | Environnement de développement |

---

## 3. Architecture générale

L'architecture repose sur une organisation distribuée dans laquelle chaque microservice possède une responsabilité spécifique.

Eureka Server centralise les informations relatives aux instances disponibles. Spring Cloud Gateway constitue le point d'entrée des requêtes HTTP, tandis que SERVICE-VOITURE communique avec SERVICE-CLIENT par l'intermédiaire d'OpenFeign.

```text
                         +--------------------+
                         |    Eureka Server   |
                         |     Port : 8761    |
                         +--------------------+
                                   |
                          Service Discovery
                                   |
                         +--------------------+
                         | Spring Cloud       |
                         | Gateway            |
                         | Port : 8888        |
                         +--------------------+
                                   |
                       +-----------+-----------+
                       |                       |
             +-------------------+   +-------------------+
             |   SERVICE-CLIENT  |   |  SERVICE-VOITURE  |
             |    Port : 8088    |   |    Port : 8089    |
             +-------------------+   +-------------------+
                       ^                       |
                       |                       |
                       +------- OpenFeign -----+
```

### Ports des applications

| Application | Port | Rôle |
|---|---|---|
| Eureka Server | 8761 | Registre des microservices |
| SERVICE-CLIENT | 8088 | Gestion des clients |
| Gateway | 8888 | Routage des requêtes |
| SERVICE-VOITURE | 8089 | Gestion des voitures |

---

## 4. Mise en place d'Eureka Server

### 4.1. Création et configuration

La première étape consiste à créer un serveur Eureka chargé de centraliser l'enregistrement des microservices.

L'application utilise la dépendance `spring-cloud-starter-netflix-eureka-server` et l'annotation `@EnableEurekaServer`.

La configuration principale est définie dans `application.properties` :

```properties
server.port=8761
spring.application.name=eureka-server

eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
```

Le serveur est configuré pour fonctionner de manière autonome, sans chercher à s'enregistrer auprès d'un autre serveur Eureka.

### 4.2. Vérification du démarrage

L'interface d'administration est accessible à l'adresse :

`http://localhost:8761/`

La capture suivante présente le tableau de bord Eureka lors de son premier démarrage, avant l'enregistrement des microservices.

<img width="1276" height="638" alt="cap1_eurika" src="https://github.com/user-attachments/assets/a71954ea-6d2f-4b71-90db-d74cb4d93e70" />


---

## 5. Création du microservice SERVICE-CLIENT

### 5.1. Configuration du microservice

SERVICE-CLIENT est développé avec Spring Boot, Spring Web, Spring Data JPA et H2 Database.

Il est configuré sur le port `8088` et utilise Eureka Discovery Client pour s'enregistrer automatiquement auprès du serveur de découverte.

```properties
server.port=8088
spring.application.name=SERVICE-CLIENT
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

### 5.2. Modèle de données

L'entité `Client` représente les informations des clients enregistrés dans la base H2.

Elle comprend les attributs suivants :

- `id` : identifiant unique.
- `nom` : nom du client.
- `age` : âge du client.

Un repository Spring Data JPA assure l'accès aux données, tandis que le contrôleur REST expose les opérations de consultation.

### 5.3. Enregistrement dans Eureka

Après le démarrage du microservice, SERVICE-CLIENT s'enregistre automatiquement dans Eureka.

La capture suivante présente son apparition dans le registre des services.

<img width="734" height="109" alt="cap2_eurika" src="https://github.com/user-attachments/assets/fc2ff083-ad72-40ed-b27d-fa4320ead8a0" />


### 5.4. Consultation des clients

Le contrôleur expose deux endpoints :

| Méthode | Endpoint | Fonction |
|---|---|---|
| GET | `/clients` | Consulter tous les clients |
| GET | `/client/{id}` | Consulter un client par identifiant |

La capture suivante illustre la réponse de l'API REST lors de la consultation de la liste des clients.

<img width="307" height="214" alt="cap3_eurika" src="https://github.com/user-attachments/assets/27a21a1a-63ff-4da7-a42e-9fd39a9e180d" />


---

## 6. Étape 5 — Configuration statique de Spring Cloud Gateway

### 6.1. Objectif

Cette étape consiste à créer une Gateway servant de point d'entrée unique pour accéder aux ressources exposées par SERVICE-CLIENT.

Dans un premier temps, le routage repose sur une configuration statique utilisant l'adresse HTTP du microservice.

La Gateway fonctionne sur le port `8888`.

### 6.2. Configuration des routes

Deux routes sont définies afin de transférer les requêtes vers SERVICE-CLIENT :

- `/clients` : consultation de la liste des clients.
- `/client/{id}` : consultation d'un client spécifique.

La destination est configurée avec l'adresse `http://localhost:8088`.

### 6.3. Test du routage vers la liste des clients

URL utilisée :

`http://localhost:8888/clients`

La capture suivante présente l'accès à la liste des clients à travers la Gateway.

<img width="785" height="125" alt="cap5_gateway_etape5" src="https://github.com/user-attachments/assets/33846c21-e6be-4c76-9e84-a0ffe1dac2ad" />


### 6.4. Test du routage vers un client

URL utilisée :

`http://localhost:8888/client/1`

La capture suivante illustre le routage d'une requête vers une ressource individuelle de SERVICE-CLIENT.

<img width="359" height="245" alt="cap5,1_gateway_etape5" src="https://github.com/user-attachments/assets/5a901c2e-997b-4f42-9d9d-d5adffb67801" />


---

## 7. Étape 6 — Configuration de Gateway avec Eureka

### 7.1. Objectif

L'utilisation d'adresses HTTP fixes crée une dépendance entre la Gateway et les emplacements des microservices.

Cette étape consiste à intégrer la Gateway à Eureka pour permettre la résolution des destinations à partir des noms des services.

Le routage utilise **Spring Cloud LoadBalancer** et le préfixe `lb://`.

### 7.2. Configuration du routage dynamique

Les routes utilisent désormais le nom logique de SERVICE-CLIENT :

```yaml
uri: lb://SERVICE-CLIENT
```

Cette configuration permet à la Gateway de rechercher les instances disponibles dans le registre Eureka.

### 7.3. Enregistrement de Gateway dans Eureka

La Gateway est configurée comme client Eureka afin de participer au mécanisme de découverte des services.

La capture suivante présente son enregistrement aux côtés de SERVICE-CLIENT.

<img width="1278" height="652" alt="cap6_gateway_eurika" src="https://github.com/user-attachments/assets/b9b49cb1-48ab-48ca-908a-66491ef1d60f" />


### 7.4. Vérification du routage dynamique

URL utilisée :

`http://localhost:8888/clients`

La capture suivante illustre l'accès à SERVICE-CLIENT après l'intégration de la Gateway avec Eureka.

<img width="936" height="151" alt="cap6 1_gateway_eurika" src="https://github.com/user-attachments/assets/c7e0801c-10b9-4c06-acd7-82cb539079ac" />


### 7.5. Accès par nom du microservice

Le TP demande également de rendre accessible SERVICE-CLIENT à travers son nom enregistré dans Eureka.

URL utilisée :

`http://localhost:8888/SERVICE-CLIENT/clients`

Une route explicite est configurée dans Spring Cloud Gateway Server Web MVC :

```yaml
- id: service-client-by-name
  uri: lb://SERVICE-CLIENT
  predicates:
    - Path=/SERVICE-CLIENT/**
  filters:
    - StripPrefix=1
```

Le filtre `StripPrefix=1` permet de retirer le segment `/SERVICE-CLIENT` avant de transmettre la requête au microservice.

La destination reste résolue dynamiquement grâce à Eureka et LoadBalancer.

La capture suivante présente l'accès au microservice à partir de son nom logique.

  <img width="421" height="374" alt="cap6 2_server_client_url" src="https://github.com/user-attachments/assets/31556a17-d64a-4503-ad5e-049880ef3e3a" />


---

## 8. Étape 7 — Création de SERVICE-VOITURE avec OpenFeign

### 8.1. Objectif

Cette étape consiste à développer un deuxième microservice métier, SERVICE-VOITURE, capable de communiquer avec SERVICE-CLIENT.

Le microservice fonctionne sur le port `8089` et utilise OpenFeign pour récupérer les informations des clients associés aux voitures.

### 8.2. Configuration du microservice

Les propriétés principales sont :

```properties
server.port=8089
spring.application.name=SERVICE-VOITURE
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

SERVICE-VOITURE utilise Spring Data JPA pour la gestion des voitures et Eureka Discovery Client pour la découverte des autres services.

### 8.3. Modèle de données

L'entité `Voiture` contient les attributs suivants :

- `id` : identifiant de la voiture.
- `marque` : marque du véhicule.
- `modele` : modèle du véhicule.
- `matricule` : numéro d'immatriculation.
- `clientId` : identifiant du client associé.

Le champ `clientId` permet d'établir une association logique avec SERVICE-CLIENT, sans relation JPA directe entre les bases de données des deux microservices.

### 8.4. Communication avec OpenFeign

La communication entre les microservices repose sur une interface déclarative OpenFeign.

```java
@FeignClient(name = "SERVICE-CLIENT")
public interface ClientFeignClient {

    @GetMapping("/client/{id}")
    Client findClientById(@PathVariable("id") Long id);
}
```

L'annotation `@FeignClient` indique le nom du microservice cible.

OpenFeign s'appuie sur Eureka et LoadBalancer pour localiser l'instance disponible et effectuer la requête HTTP.

### 8.5. Enregistrement dans Eureka

Après le démarrage de SERVICE-VOITURE, les trois applications clientes sont visibles dans le registre Eureka.

La capture suivante présente l'ensemble des services enregistrés.

<img width="1280" height="703" alt="cap7_Eureka_Trois_Microservices" src="https://github.com/user-attachments/assets/cd1e8a7e-d230-4b84-b27d-12e4be2c4ff4" />


### 8.6. Consultation des voitures avec leurs clients

Endpoint utilisé :

`GET http://localhost:8089/voitures`

Le contrôleur récupère les voitures depuis la base H2. Le service utilise ensuite OpenFeign pour obtenir les informations des clients associés.

La capture suivante présente la réponse de l'API contenant les voitures et leurs informations client.

<img width="396" height="683" alt="cap7 1_Service_Voiture_Liste_OpenFeign" src="https://github.com/user-attachments/assets/d295dd04-c878-40d4-93ac-25377d738d9c" />


### 8.7. Consultation d'une voiture

Endpoint utilisé :

`GET http://localhost:8089/voitures/1`

Cette opération permet de récupérer les détails d'une voiture ainsi que les informations du client correspondant.

La capture suivante illustre la réponse obtenue à travers la communication entre les deux microservices.

<img width="299" height="307" alt="cap7 2_voiture_1" src="https://github.com/user-attachments/assets/22aeb54f-4a2d-46b3-a0ef-a93e746be7b4" />


---

## 9. Étape 8 — Implémentation du contrôleur Voiture

### 9.1. Objectif

Cette étape consiste à compléter le contrôleur REST de SERVICE-VOITURE avec les opérations de recherche, d'ajout et de modification des voitures.

Le contrôleur utilise les composants existants :

- `VoitureRepository` pour l'accès aux données.
- `VoitureService` pour la logique métier.
- `ClientFeignClient` pour la communication avec SERVICE-CLIENT.

### 9.2. Endpoints du contrôleur

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/voitures` | Consulter toutes les voitures |
| GET | `/voitures/{id}` | Consulter une voiture |
| GET | `/voitures/client/{id}` | Rechercher les voitures d'un client |
| POST | `/voitures/{clientId}` | Enregistrer une voiture |
| PUT | `/voitures/{id}` | Modifier une voiture |

### 9.3. Recherche par client

L'endpoint `/voitures/client/{id}` permet de récupérer les voitures associées à un client.

Le contrôleur vérifie l'existence du client à travers OpenFeign avant de rechercher les voitures correspondantes dans le repository.

### 9.4. Ajout d'une voiture

L'endpoint `POST /voitures/{clientId}` permet d'enregistrer une nouvelle voiture.

L'identifiant du client est récupéré depuis l'URL. Le contrôleur vérifie son existence auprès de SERVICE-CLIENT avant d'effectuer l'enregistrement.

### 9.5. Modification d'une voiture

L'endpoint `PUT /voitures/{id}` permet de modifier les informations d'une voiture existante.

Les attributs concernés sont :

- La marque.
- Le modèle.
- Le matricule.

L'association avec le client est conservée pendant la modification.

### 9.6. Gestion des erreurs

Une gestion des exceptions est prévue afin de distinguer les erreurs liées aux données, aux ressources inexistantes et à l'indisponibilité d'un microservice.

Les codes HTTP prévus comprennent notamment `400`, `404`, `409` et `503`.

**État de l'étape :** les opérations supplémentaires ont été ajoutées au projet. La validation finale des tests Maven et HTTP reste à confirmer après l'interruption de la session de développement.

---

## 10. Vérification et tests

La validation des différentes étapes repose sur deux types de vérifications :

- **Tests automatisés Maven :** compilation, chargement du contexte Spring et vérification des composants.
- **Tests fonctionnels HTTP :** consultation des endpoints REST et vérification du routage et de la communication entre microservices.

Les étapes précédentes ont permis de tester :

- Le démarrage d'Eureka Server.
- L'enregistrement des microservices.
- Les endpoints REST de SERVICE-CLIENT.
- Le routage statique de Spring Cloud Gateway.
- Le routage dynamique avec Eureka.
- L'accès aux services par leur nom logique.
- La communication interservices avec OpenFeign.

Les captures présentées dans les sections précédentes documentent les principales vérifications fonctionnelles.

---

## 11. Exécution des applications

Pour démarrer l'architecture, lancer les applications dans l'ordre suivant :

1. **Eureka Server** — port 8761.
2. **SERVICE-CLIENT** — port 8088.
3. **Gateway** — port 8888.
4. **SERVICE-VOITURE** — port 8089.

Chaque application peut être exécutée depuis IntelliJ IDEA à travers sa classe principale Spring Boot.

### URLs principales

| Service | URL |
|---|---|
| Eureka Server | http://localhost:8761/ |
| SERVICE-CLIENT | http://localhost:8088/clients |
| Client par ID | http://localhost:8088/client/1 |
| Gateway | http://localhost:8888/clients |
| Gateway par nom Eureka | http://localhost:8888/SERVICE-CLIENT/clients |
| SERVICE-VOITURE | http://localhost:8089/voitures |
| Voiture par ID | http://localhost:8089/voitures/1 |

---

## 12. Conclusion

Ce travail pratique a porté sur la mise en œuvre d'une architecture microservices utilisant Spring Boot et Spring Cloud.

**Netflix Eureka** assure la découverte et l'enregistrement des services. **Spring Cloud Gateway** permet de centraliser les requêtes HTTP et de mettre en place un routage statique ou dynamique.

L'utilisation de **Spring Cloud LoadBalancer** permet de résoudre les instances à partir de leurs noms logiques, tandis que **OpenFeign** facilite la communication entre SERVICE-VOITURE et SERVICE-CLIENT.

L'ensemble illustre les principaux mécanismes d'une architecture distribuée : séparation des responsabilités, découverte des services, routage centralisé et communication interservices.

---

**Réalisé par : Amine CHEKKOURI**  
**5IIR — EMSI Marrakech**  
**Année universitaire : 2026–2027**
