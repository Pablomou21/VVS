import {useSelector, useDispatch} from 'react-redux';
import Movies from './Movies';
import * as selectors from '../selectors';
import DateSelector from "./DateSelector";
import backend from '../../../backend';
import catalog from '../../catalog';
import * as actions from '../actions';

const Billboard = () => {
    const movies = useSelector(selectors.getMovies);
    const billboardDate = useSelector(selectors.getBillboardDate);
    const dispatch = useDispatch();

    const handleBillboardDateChange = async date => {
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        const selectedDate = new Date(date);
        selectedDate.setHours(0, 0, 0, 0);

        const diffInMilliseconds = selectedDate.getTime() - today.getTime();
        const daysOffset = Math.round(diffInMilliseconds / (1000 * 60 * 60 * 24));

        const safeDaysOffset = Math.max(0, Math.min(6, daysOffset));

        dispatch(catalog.actions.clearBillboard(date));
        const response = await backend.catalogService.getBillboard(safeDaysOffset);
        if (response.ok) {
            dispatch(actions.getBillboardCompleted(response.payload));
        }
    }

    return (
        <div>
            <DateSelector id="billboardDate" className="mb-2 w-auto"
                 value={billboardDate} onChange={e => handleBillboardDateChange(e.target.value)} />
            <Movies movies={movies}/>
        </div>
    );

}

export default Billboard;