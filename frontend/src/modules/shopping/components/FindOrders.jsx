import {useState, useEffect} from 'react';
import {FormattedMessage} from 'react-intl';
import Alert from 'react-bootstrap/Alert';

import backend from '../../../backend';
import Orders from './Orders';
import { Pager } from '../../common';

const FindOrders = () => {
    const [orders, setOrders] = useState([]);
    const [page, setPage] = useState(0);
    const [existMoreItems, setExistMoreItems] = useState(false);

    useEffect(() => {
        const fetchOrders = async () => {
            const response = await backend.shoppingService.findOrders({ page: page });

            if (response.ok) {
                setOrders(response.payload.items);
                setExistMoreItems(response.payload.existMoreItems);
            }
        }
        fetchOrders();

    }, [page]);

    if (orders.length === 0 && page === 0) {
        return (
            <Alert variant="info">
                <FormattedMessage id="project.shopping.FindOrders.noOrders"/>
            </Alert>
        );
    }

    return (
        <div>
            <Orders orders={orders} />
            <Pager
                back={{
                    enabled: page >= 1,
                    onClick: () => setPage(page - 1)
                }}
                next={{
                    enabled: existMoreItems,
                    onClick: () => setPage(page + 1)
                }}
            />
        </div>
    );
};

export default FindOrders;