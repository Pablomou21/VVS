import * as actionTypes from './actionTypes';

export const buyCompleted = (orderId) => ({
    type: actionTypes.BUY_COMPLETED,
    orderId
});