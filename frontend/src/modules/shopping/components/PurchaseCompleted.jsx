import {useSelector} from 'react-redux';
import {FormattedMessage} from 'react-intl';
import Alert from 'react-bootstrap/Alert';

import * as selectors from '../selectors';
import {BackLink} from "../../common/index.js";

const PurchaseCompleted = () => {

    const orderId = useSelector(selectors.getLastOrderId);

    if (!orderId) {
        return null;
    }

    return (
        <Alert variant="success">
            <FormattedMessage id="project.shopping.PurchaseCompleted.purchaseOrderGenerated"/>:
            &nbsp;
            <span id="purchaseOrderId">{orderId}</span>
            <div className="mt-3">

                <BackLink/>

            </div>
        </Alert>

    );

}

export default PurchaseCompleted;