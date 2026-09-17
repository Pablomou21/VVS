import {useEffect} from 'react';
import {useDispatch} from 'react-redux';

import Header from './Header';
import Body from './Body';
import Footer from './Footer';
import users from '../../users';
import backend from '../../../backend';
import catalog from '../../catalog';

const App = () => {

    const dispatch = useDispatch();
    const today = 0;

    useEffect(() => {

        const tryLoginFromServiceToken = async () => {
            const response = await backend.userService.tryLoginFromServiceToken(
                () => dispatch(users.actions.logout()));
            if (response.ok) {
                dispatch(users.actions.loginCompleted(response.payload));
            }
        }

        const getBillboard = async () => {
            const response = await backend.catalogService.getBillboard(today);
            if (response.ok) {
                dispatch(catalog.actions.getBillboardCompleted(response.payload));
            }
        }

        tryLoginFromServiceToken();
        getBillboard();
    
    }, [dispatch]);

    return (
        <div className="d-flex flex-column min-vh-100">
            <Header/>
            <Body/>
            <Footer/>
        </div>
    );

}
    
export default App;
