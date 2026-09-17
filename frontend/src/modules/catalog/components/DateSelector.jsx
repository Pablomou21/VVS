import {FormattedDate} from 'react-intl';
import Form from 'react-bootstrap/Form';

const DateSelector = (selectProps) => {

    const dates = [];
    const currentDate = new Date();
    let index;
    for (index=0; index <7; index++) {
        dates[index] = addDaysToDate(currentDate, index);
    }

    return (

        <Form.Select {...selectProps}>

            {dates.map(date =>
                <FormattedDate value={date} key={date.getTime()}>
                    {dateAsString => (<option value={isoDate(date)}>{dateAsString}</option>)}
                </FormattedDate>
            )}

        </Form.Select>
    );

}

const addDaysToDate = (date, days) => {
    let newDate = new Date(date);
    newDate.setDate(date.getDate() + days);
    return newDate;
}

const isoDate = date => {
    let day = date.getDate();
    let month = date.getMonth() + 1;
    let year = date.getFullYear();
    return `${year}-${month<10?`0${month}`:`${month}`}-${day<10?`0${day}`:`${day}`}`;
}

export default DateSelector;