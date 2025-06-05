package server.protocol;

/**
 * Constants for client-server communication protocol
 */
public final class ServerProtocol {
    // Avoid instantiation
    private ServerProtocol() {}

    // Authentication commands
    public static final String CMD_LOGIN = "LOGIN";
    public static final String CMD_CREATE_USER = "CREATE";
    public static final String CMD_LOGOUT = "LOGOUT";

    // Authentication responses
    public static final String RESP_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String RESP_LOGIN_FAIL = "LOGIN_FAIL";
    public static final String RESP_CREATE_SUCCESS = "CREATE_SUCCESS";
    public static final String RESP_CREATE_FAIL = "CREATE_FAIL";

    // Playlist controls
    public static final String CMD_CREATE_PLAYLIST = "CREATE_PLAYLIST";
    public static final String CMD_GET_PLAYLISTS = "GET_PLAYLISTS";
    public static final String CMD_GET_PLAYLIST = "GET_PLAYLIST";
    public static final String CMD_CHECK_PLAYLIST = "CHECK_PLAYLIST";
    public static final String CMD_ADD_SONG_TO_PLAYLIST = "ADD_SONG_TO_PLAYLIST";
    public static final String CMD_REMOVE_SONG_FROM_PLAYLIST = "REMOVE_SONG_FROM_PLAYLIST";

    // Playlist answers
    public static final String RESP_PLAYLIST_CREATED = "PLAYLIST_CREATED";
    public static final String RESP_PLAYLIST_EXISTS = "PLAYLIST_EXISTS";
    public static final String RESP_PLAYLIST_FOUND = "PLAYLIST_FOUND";
    public static final String RESP_PLAYLIST_NOT_FOUND = "PLAYLIST_NOT_FOUND";

    // Music library controls
    public static final String CMD_GET_ALL_SONGS = "GET_ALL_SONGS";
    public static final String CMD_SEARCH_TITLE = "SEARCH_TITLE";
    public static final String CMD_SEARCH_ARTIST = "SEARCH_ARTIST";

    // Playback controls
    public static final String CMD_LOAD_PLAYLIST = "LOAD_PLAYLIST";
    public static final String CMD_SET_PLAYBACK_MODE = "SET_PLAYBACK_MODE";
    public static final String CMD_PLAYER_PLAY = "PLAYER_PLAY";
    public static final String CMD_PLAYER_PAUSE = "PLAYER_PAUSE";
    public static final String CMD_PLAYER_STOP = "PLAYER_STOP";
    public static final String CMD_PLAYER_NEXT = "PLAYER_NEXT";
    public static final String CMD_PLAYER_PREV = "PLAYER_PREV";
    public static final String CMD_PLAYER_EXIT = "PLAYER_EXIT";

    // Generic answers
    public static final String RESP_SUCCESS = "SUCCESS";
    public static final String RESP_ERROR = "ERROR";
    public static final String RESP_END = "END";
}