const getModuleState = state => state.users;

export const getUser = state =>
    getModuleState(state).user;

export const isLoggedIn = state =>
    getUser(state) !== null;

export const getUserName = state =>
    isLoggedIn(state) ? getUser(state).userName : null;

export const getUserRole = state =>
    isLoggedIn(state) ? getUser(state).role : null;

export const isViewer = state =>
    getUserRole(state) === 'USER';

export const isClerk = state =>
    getUserRole(state) === 'CLERK';