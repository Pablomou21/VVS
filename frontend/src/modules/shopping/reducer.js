import {combineReducers} from 'redux';

import * as actionTypes from './actionTypes';

const initialState = {
    lastOrderId: null,
};

const lastOrderId = (state = initialState.lastOrderId, action) => {

    switch (action.type) {

        case actionTypes.BUY_COMPLETED:
            return action.orderId;

        default:
            return state;

    }

}

const reducer = combineReducers({
    lastOrderId,
});

export default reducer;
