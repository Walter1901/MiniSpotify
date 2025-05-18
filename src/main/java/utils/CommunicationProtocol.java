package utils;

public interface CommunicationProtocol {
    // Commandes d'authentification
    String LOGIN_COMMAND = "LOGIN";
    String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    String LOGIN_FAIL = "LOGIN_FAIL";
    String CREATE_COMMAND = "CREATE";
    String CREATE_SUCCESS = "CREATE_SUCCESS";
    String CREATE_FAIL = "CREATE_FAIL";
    String LOGOUT_COMMAND = "LOGOUT";

    // Commandes de playlist
    String CREATE_PLAYLIST = "CREATE_PLAYLIST";
    String PLAYLIST_CREATED = "PLAYLIST_CREATED";
    String PLAYLIST_EXISTS = "PLAYLIST_EXISTS";
    String GET_PLAYLISTS = "GET_PLAYLISTS";
    String GET_PLAYLIST = "GET_PLAYLIST";
    String CHECK_PLAYLIST = "CHECK_PLAYLIST";
    String PLAYLIST_FOUND = "PLAYLIST_FOUND";
    String PLAYLIST_NOT_FOUND = "PLAYLIST_NOT_FOUND";

    // Commandes de chansons
    String GET_ALL_SONGS = "GET_ALL_SONGS";
    String SEARCH_TITLE = "SEARCH_TITLE";
    String SEARCH_ARTIST = "SEARCH_ARTIST";
    String ADD_SONG_TO_PLAYLIST = "ADD_SONG_TO_PLAYLIST";
    String REMOVE_SONG_FROM_PLAYLIST = "REMOVE_SONG_FROM_PLAYLIST";

    // Commandes du lecteur
    String LOAD_PLAYLIST = "LOAD_PLAYLIST";
    String SET_PLAYBACK_MODE = "SET_PLAYBACK_MODE";
    String PLAYER_PLAY = "PLAYER_PLAY";
    String PLAYER_PAUSE = "PLAYER_PAUSE";
    String PLAYER_STOP = "PLAYER_STOP";
    String PLAYER_NEXT = "PLAYER_NEXT";
    String PLAYER_PREV = "PLAYER_PREV";
    String PLAYER_EXIT = "PLAYER_EXIT";

    // Marqueur de fin de réponse
    String END = "END";

    // Préfixes d'erreur
    String ERROR = "ERROR";
    String SUCCESS = "SUCCESS";
}
