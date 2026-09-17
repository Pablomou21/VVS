import {appFetch} from './appFetch';

export const findOrders = async ({page}) =>
    await appFetch('GET', `/orders?page=${page}`);

export const buyTickets = async (sessionId, numTickets, creditCardNum) =>
    await appFetch('POST', '/orders', {
        sessionId,
        numTickets,
        creditCardNum
    });

export const deliverTickets = async (orderId, creditCardNum) =>
    await appFetch('POST', `/orders/${orderId}/deliver`, {creditCardNum});