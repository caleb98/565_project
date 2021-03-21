# 565_project

### Steps to run:
1. Clone the repository: `git clone https://github.com/caleb98/565_project.git`
2. Inside project folder, run `gradlew run` on windows, or `./gradlew run` on unix.

### Notes
The `Request Pet Data` function of this program accesses the BattleNet World of Warcraft public API. 
These requests require a client ID and client secret value obtained by setting up a client on [the BattleNet dev site](develop.battle.net).
This program will attempt to read the client ID and client secret values from the system environment variables using the names `BNET_CLIENT_ID` and `BNET_CLIENT_SECRET`.
Trying to run this function without both of these environment variables set will produce and error message and no http requests will be made.
