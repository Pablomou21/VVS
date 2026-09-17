import * as actionTypes from './actionTypes';

export const getBillboardCompleted = billboard => ({
    type: actionTypes.GET_BILLBOARD_COMPLETED,
    billboard
});

export const clearBillboard = billboardDate => ({
    type: actionTypes.CLEAR_BILLBOARD,
    billboardDate
});