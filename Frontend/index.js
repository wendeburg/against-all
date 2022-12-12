(async function() {
    const port = prompt("Introduce el puerto de API_Engine");

    const baseUrl = "https://localhost:" + port + "/";

    const body = document.querySelector("#body");
    const subtitleContainer = document.querySelector("#subtitle-container");

    let spectatedMatches = [];

    async function getGameID() {
        const res = await fetch(baseUrl);

        return res.json();   
    }

    async function getGameState() {
        const res = await fetch(baseUrl + "gamestate");

        return res.json();
    }

    async function getCtities() {
        const res = await fetch(baseUrl + "city");

        return res.json();
    }

    async function getMap() {
        const res = await fetch(baseUrl + "map");

        return res.json();
    }

    async function getPlayers() {
        const res = await fetch(baseUrl + "players");

        return res.json();
    }

    async function getNPCs() {
        const res = await fetch(baseUrl + "npcs");

        return res.json();
    }

    function getErrorMessage(message) {
        let newElement = document.createElement('p');
        newElement.textContent = message;
        newElement.classList.add("initial-message");
        newElement.classList.add("error-message");

        return newElement; 
    }

    function removeAllChildNodes(parent) {
        while (parent.firstChild) {
            parent.removeChild(parent.firstChild);
        }
    }

    function setCellColor(i, j, cell) {
        if (i < 10 && j < 10) {
            cell.classList.add("city1");
        }
        else if (i >= 10 && j < 10) {
            cell.classList.add("city2");
        }
        else if (i < 10 && j >= 10) {
            cell.classList.add("city3");
        }
        else {
            cell.classList.add("city4");
        }
    }

    function drawInitialBoard() {
        let boardWrapper = document.createElement("div");
        boardWrapper.classList.add("info-container-wrapper");
        boardWrapper.setAttribute("style", "margin-left: 50px;")

        let board = document.createElement('div');
        board.setAttribute("id", "board");

        for (let i = 0; i < 20; i++) {
            for (let j = 0; j < 20; j++) {
                let newCell = document.createElement('div');
                newCell.classList.add("board-cell");

                let newCellStyle = "";

                if (j != 0) {
                    newCellStyle = newCellStyle + " border-left: 1px solid #000000;";
                }
                if (j != 19) {
                    newCellStyle = newCellStyle + " border-right: 1px solid #000000;";
                }

                if (i != 0) {
                    newCellStyle = newCellStyle + " border-top: 1px solid #000000;";
                }
                if (i != 19) {
                    newCellStyle = newCellStyle + " border-botom: 1px solid #000000;";
                }

                newCell.setAttribute("style", newCellStyle);

                setCellColor(i, j, newCell)
                
                board.appendChild(newCell);
            }
        }

        let boardTitle = document.createElement("h3");
        boardTitle.classList.add("info-container-title");
        boardTitle.textContent = "Map";

        boardWrapper.appendChild(boardTitle);
        boardWrapper.appendChild(board);
        body.appendChild(boardWrapper);
    }

    function drawInfoContainer(title) {
        let newInfoWrapper = document.createElement("div");
        newInfoWrapper.classList.add("info-container-wrapper");

        let newInfoTitle = document.createElement("h3");
        newInfoTitle.classList.add("info-container-title");
        newInfoTitle.textContent = title;

        let newInfoContainer = document.createElement("div");
        newInfoContainer.classList.add("info-container");

        newInfoWrapper.appendChild(newInfoTitle);
        newInfoWrapper.appendChild(newInfoContainer);

        body.appendChild(newInfoWrapper);
    }

    function drawControlPanel() {
        drawInitialBoard();

        drawInfoContainer("Players");

        drawInfoContainer("NPCs");

        drawInfoContainer("Cities");
    }

    function addCitiesToInfoContainer(cities) {
        let cityClasses = ["city1", "city2", "city3", "city4"];

        let citiesInfoContainer = body.children[3].children[1];

        let counter = 0;
        for (let [key, value] of Object.entries(cities)) {
            let newCityInfo = document.createElement("p");
            newCityInfo.classList.add("city-info");
            newCityInfo.classList.add(cityClasses[counter]);
            newCityInfo.textContent = `${key}: ${value}`;

            citiesInfoContainer.appendChild(newCityInfo);

            counter++;
        }
    }

    function updateMap(map, players) {
        const cells = body.children[0].children[1].children;

        for (let i = 0; i < 20; i++) {
            for (let j = 0; j < 20; j++) {
                const currentCell = cells[i*20 + j];

                let elementInCellStr = "";
                const elementsArray = map[i][j];

                for (let k = 0; k < elementsArray.length; k++) {
                    if (elementsArray[k] == 1) {
                        // Alimento.
                        elementInCellStr = "A";
                        break;
                    }
                    else if (elementsArray[k] == 0) {
                        // Espacio vacío.
                        elementInCellStr = "";
                    }
                    else if (elementsArray[k] == 2) {
                        // Mina.
                        elementInCellStr = "M";
                    }
                    else {
                        // Jugador/NPC.
                        elementInCellStr = "P"
                    }
                }

                removeAllChildNodes(currentCell);

                if (elementInCellStr !== "") {
                    let newCellElement = document.createElement("p");
                    newCellElement.classList.add("board-cell-element");
                    newCellElement.textContent = elementInCellStr;
                    
                    switch (elementInCellStr) {
                        case "M": newCellElement.classList.add("mine");
                                  break;
                        case "A": newCellElement.classList.add("food");
                                  break;
                        case "P": newCellElement.classList.add("player");
                                  break;
                    }
    
                    currentCell.appendChild(newCellElement);
                }
            }
        }
    }

    function updatePlayersAndNPCs(characters, infoContainer, updatingPlayers) {
        removeAllChildNodes(infoContainer);

        if (Object.keys(characters).length === 0) {
            let noCharactersMessage = document.createElement("p");
            noCharactersMessage.classList.add("no-characters-message");
            noCharactersMessage.textContent = "There are no " + (updatingPlayers? "players " : "NPCs ") + "in this game"

            infoContainer.appendChild(noCharactersMessage);
        }

        for (let [key, value] of Object.entries(characters)) {
            let playerInfoWrapper = document.createElement("div");
            playerInfoWrapper.classList.add("player-info-wrapper");

            let playerName = document.createElement("p");
            playerName.classList.add("player-info-name");
            playerName.textContent = key;

            let playerLevel = document.createElement("p");
            playerLevel.classList.add("player-info-level");
            playerLevel.textContent = "Level: " + value["nivel"];

            let playerPos = document.createElement("p");
            playerPos.classList.add("player-info-pos");
            playerPos.textContent = "Posiion: " + "[" + value["posicion"] + "]";

            playerInfoWrapper.appendChild(playerName);
            playerInfoWrapper.appendChild(playerLevel);
            playerInfoWrapper.appendChild(playerPos);

            infoContainer.appendChild(playerInfoWrapper);
        }
    }

    function showRestartBtn() {
        let restartBtn = document.createElement("button");
        restartBtn.innerText = "Restart";
        restartBtn.setAttribute("id", "restart-btn");

        subtitleContainer.appendChild(restartBtn);

        const btnElement = subtitleContainer.querySelector("#restart-btn");
        btnElement.addEventListener("click", function() {
            removeAllChildNodes(body);
            removeAllChildNodes(subtitleContainer);

            let loadingMsg = document.createElement("p");
            loadingMsg.textContent = "Loading match data...";
            loadingMsg.setAttribute("id", "loading-message");
            loadingMsg.classList.add("initial-message");
            body.appendChild(loadingMsg);

            body.classList.add("display-message");

            main();
        });
    }

    function showWinners(winners) {
        for (let i = 0; i < winners.length; i++) {
            const playerInfoWrappers = body.children[1].children[1].children;

            for (let j = 0; j < playerInfoWrappers.length; j++) {
                if (playerInfoWrappers[j].children[0].textContent == winners[i]) {
                    playerInfoWrappers[j].classList.add("winner");
                }
            }
        }
    }

    async function gameSpactateHandler(initialRequest) {
        removeAllChildNodes(body);
        body.className = "";
    
        const gameID = document.createElement('h2');
        gameID.setAttribute("id", "gameid");
        gameID.textContent = "Game ID: "+ initialRequest["idpartida"];
    
        subtitleContainer.appendChild(gameID);
    
        drawControlPanel();
    
        const citiesRequest = await getCtities();
    
        addCitiesToInfoContainer(citiesRequest.cities);
    
        let gameStateRequest = await getGameState();
    
        const gameInterval = setInterval(async function() {
            try {
                const playerRequest = await getPlayers();
                const npcsRequest = await getNPCs();
                const mapRequest = await getMap();
        
                updateMap(mapRequest.map);
        
                updatePlayersAndNPCs(playerRequest.players, body.children[1].children[1], true);
        
                updatePlayersAndNPCs(npcsRequest.npcs, body.children[2].children[1], false);
        
                gameStateRequest = await getGameState();

                const errorMsg = subtitleContainer.children[1];

                if (errorMsg && errorMsg.id === "subtitle-error-message") {
                    subtitleContainer.removeChild(errorMsg);
                }
            }
            catch (err) {
                if (!subtitleContainer.querySelector(".error-message")) {
                    let errorMsg = document.createElement("p");
                    errorMsg.textContent = "An error has ocurred while retrieving match data. Retrying...";
                    errorMsg.classList.add("error-message");
                    errorMsg.setAttribute("id", "subtitle-error-message");
    
                    subtitleContainer.appendChild(errorMsg);
                }
            }
    
            if (gameStateRequest["gamefinished"] === true) {
                clearInterval(gameInterval);
                showRestartBtn();
                showWinners(gameStateRequest["winners"]);
            }
        }, 500);
    }

    async function main() {
        let initialRequest = {};

        const initialInterval = setInterval(async function() {
            try {
                initialRequest = await getGameID();
            }
            catch (err) {
                const loadingMessage = body.querySelector("#loading-message");
    
                if (Array.from(body.childNodes).includes(loadingMessage)) {
                    body.removeChild(loadingMessage);
    
                    body.appendChild(getErrorMessage("There has been an error while retrieving game data. Retrying..."));
                }
            }

            if (initialRequest["idpartida"] !== undefined && !spectatedMatches.includes(initialRequest["idpartida"])) {
                spectatedMatches.push(initialRequest["idpartida"]);
                gameSpactateHandler(initialRequest);
                clearInterval(initialInterval);
            }
        }, 1000);
    }

    main();

    // TODO.
    // Mostrar ganadores. Y Reiniciar buel buscando nueva partida.
    // Hacer que al poner el mouse encima de un jugador te de infomación del jugador.
}
)();