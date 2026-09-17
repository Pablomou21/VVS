const getModuleState = state => state.catalog;

export const getMovies = state =>
    getModuleState(state).billboard;

export const getBillboardDate = state =>
    getModuleState(state).billboardDate;