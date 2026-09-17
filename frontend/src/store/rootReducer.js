import {combineReducers} from 'redux';

import app from '../modules/app';
import users from '../modules/users';
import catalog from '../modules/catalog';
import shopping from '../modules/shopping';

const rootReducer = combineReducers({
    app: app.reducer,
    users: users.reducer,
    catalog: catalog.reducer,
    shopping: shopping.reducer
});

export default rootReducer;
