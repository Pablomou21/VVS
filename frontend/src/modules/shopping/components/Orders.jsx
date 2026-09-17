import {FormattedMessage, FormattedDate, FormattedTime, FormattedNumber} from 'react-intl';
import Table from 'react-bootstrap/Table';

const Orders = ({orders}) => (

    <Table striped hover>

        <thead>
        <tr>
            <th><FormattedMessage id='project.global.fields.orderDate'/></th>
            <th><FormattedMessage id='project.global.fields.id'/></th>
            <th><FormattedMessage id='project.global.fields.title'/></th>
            <th><FormattedMessage id='project.global.fields.tickets'/></th>
            <th><FormattedMessage id='project.global.fields.totalPrice'/></th>
            <th><FormattedMessage id='project.global.fields.sessionDate'/></th>
            <th><FormattedMessage id='project.global.fields.delivered'/></th>
        </tr>
        </thead>

        <tbody>
        {orders.map((order, index) =>
            <tr key={order.id}>
                <td><FormattedDate value={new Date(order.date)}/> - <FormattedTime value={new Date(order.date)}/></td>
                <td id={`orderId-${index}`}>{order.id}</td>
                <td id={`orderMovieTitle-${index}`}>{order.movieTitle}</td>
                <td>{order.units}</td>
                <td><FormattedNumber value={order.totalPrice} style="currency" currency="EUR"/></td>
                <td><FormattedDate value={new Date(order.sessionDate)}/> - <FormattedTime value={new Date(order.sessionDate)}/></td>
                <td>
                    {order.delivered ? (
                        <FormattedMessage id='project.global.fields.yes'/>
                    ) : (
                        <FormattedMessage id='project.global.fields.no'/>
                    )}
                </td>

            </tr>
        )}
        </tbody>

    </Table>

);

export default Orders;