import {useState} from 'react';
import {FormattedMessage} from 'react-intl';
import Card from 'react-bootstrap/Card';
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';

import {BackLink, Errors, Success} from '../../common';
import backend from '../../../backend';

const DeliverForm = () => {

    const [orderId, setOrderId] = useState('');
    const [creditCardNum, setCreditCardNum] = useState('');
    const [backendErrors, setBackendErrors] = useState(null);
    const [successMessage, setSuccessMessage] = useState(null);
    const [formValidated, setFormValidated] = useState(false);
    let form;

    const handleSubmit = async event => {

        event.preventDefault();

        if (form.checkValidity()) {

            const response = await backend.shoppingService.deliverTickets(
                orderId, creditCardNum);

            if (response.ok) {
                setSuccessMessage(<FormattedMessage id="project.shopping.DeliverForm.ticketsDelivered"/>);
                setBackendErrors(null);
            } else {
                setBackendErrors(response.payload);
                setSuccessMessage(null);
            }

        } else {
            setSuccessMessage(null);
            setBackendErrors(null);
            setFormValidated(true);
        }

    }

    return (

        <div className="col-md-10 mx-auto">
            <Errors errors={backendErrors}
                    onClose={() => setBackendErrors(null)}
                    id="errorMessage"/>
            <Card className="bg-light border-dark">
                <Card.Header as="h5">
                    <FormattedMessage id="project.shopping.DeliverForm.title"/>
                </Card.Header>
                <Card.Body>
                    <Form ref={node => form = node}
                          validated={formValidated} noValidate
                          onSubmit={(e) => handleSubmit(e)}>
                        <Form.Group as={Row} className="mb-3" controlId="orderId">
                            <Form.Label column md={3}>
                                <FormattedMessage id="project.global.fields.orderId"/>
                            </Form.Label>
                            <Col md={4}>
                                <Form.Control type="number"
                                              value={orderId}
                                              onChange={e => setOrderId(e.target.value)}
                                              autoFocus
                                              required/>
                                <Form.Control.Feedback type="invalid">
                                    <FormattedMessage id='project.global.validator.number'/>
                                </Form.Control.Feedback>
                            </Col>
                        </Form.Group>
                        <Form.Group as={Row} className="mb-3" controlId="creditCardNum">
                            <Form.Label column md={3}>
                                <FormattedMessage id="project.global.fields.creditCardNum"/>
                            </Form.Label>
                            <Col md={4}>
                                <Form.Control type="text"
                                              value={creditCardNum}
                                              onChange={e => setCreditCardNum(e.target.value)}
                                              required/>
                                <Form.Control.Feedback type="invalid">
                                    <FormattedMessage id='project.global.validator.required'/>
                                </Form.Control.Feedback>
                            </Col>
                        </Form.Group>
                        <Form.Group as={Row}>
                            <Col md={{ span: 4, offset: 3 }}>
                                <Button type="submit" id="deliverSubmitButton">
                                    <FormattedMessage id="project.global.buttons.Deliver"/>
                                </Button>
                            </Col>
                        </Form.Group>
                    </Form>
                    <div className="mt-3">

                        <BackLink/>

                    </div>
                </Card.Body>
            </Card>
            <Success
                message={successMessage}
                    onClose={() => setSuccessMessage(null)}
                    id="successMessage"/>
        </div>

    );

}

export default DeliverForm;
