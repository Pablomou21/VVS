import {appFetch} from './appFetch';

export const getBillboard = async day =>
    await appFetch('GET', `/movies/billboard/${day}`);

export const getMovieDetails = async id =>
    await appFetch('GET', `/movies/${id}`);

export const getSessionDetails = async id =>
    await appFetch('GET', `/movies/sessions/${id}`);