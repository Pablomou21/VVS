import {useState} from 'react';
import {useDispatch} from 'react-redux';
import {useNavigate} from 'react-router';
import {FormattedMessage} from 'react-intl';
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';

import backend from '../../../backend';
import * as actions from '../actions';
import {Errors} from '../../common';

const BuyTicketsForm = ({sessionId}) => {

    const dispatch = useDispatch();
    const navigate = useNavigate();
    const [numTickets, setNumTickets] = useState(1);
    const [creditCardNum, setCreditCardNum] = useState('');
    const [formValidated, setFormValidated] = useState(false);
    const [backendErrors, setBackendErrors] = useState(null);

    let form;

    const handleSubmit = async event => {

        event.preventDefault();

        if (form.checkValidity()) {

            const response = await backend.shoppingService.buyTickets(
                sessionId,
                numTickets,
                creditCardNum
            );

            if (response.ok) {
                dispatch(actions.buyCompleted(response.payload));
                navigate('/shopping/purchase-completed');
            } else {
                setBackendErrors(response.payload);
            }

        } else {
            setBackendErrors(null);
            setFormValidated(true);
        }

    }

    return (

        <div id="buyTicketsForm" className="mt-4">

            <h5>
                <FormattedMessage id="project.shopping.BuyTicketsForm.title"/>
            </h5>

            <Errors errors={backendErrors}
                    onClose={() => setBackendErrors(null)}/>

            <Form ref={node => form = node}
                  noValidate
                  validated={formValidated}
                  onSubmit={event => handleSubmit(event)}>

                <Form.Group as={Row}
                            className="mb-3"
                            controlId="numTickets">

                    <Form.Label column md={3}>
                        <FormattedMessage id="project.shopping.BuyTicketsForm.fields.numTickets"/>
                    </Form.Label>

                    <Col md={4}>

                        <Form.Control type="number"
                                      min="1"
                                      max="10"
                                      value={numTickets}
                                      onChange={e => setNumTickets(e.target.value)}
                                      required/>

                        <Form.Control.Feedback type="invalid">
                            <FormattedMessage id="project.shopping.BuyTicketsForm.validation.numTickets"/>
                        </Form.Control.Feedback>

                    </Col>

                </Form.Group>

                <Form.Group as={Row}
                            className="mb-3"
                            controlId="creditCardNum">

                    <Form.Label column md={3}>
                        <FormattedMessage id="project.shopping.BuyTicketsForm.fields.creditCardNum"/>
                    </Form.Label>

                    <Col md={4}>

                        <Form.Control type="text"
                                      value={creditCardNum}
                                      onChange={e => setCreditCardNum(e.target.value)}
                                      required/>

                        <Form.Control.Feedback type="invalid">
                            <FormattedMessage id="project.global.validator.required"/>
                        </Form.Control.Feedback>

                    </Col>

                </Form.Group>

                <Form.Group as={Row}>

                    <Col md={{span: 4, offset: 3}}>

                        <Button id="buyButton" type="submit">
                            <FormattedMessage id="project.shopping.BuyTicketsForm.buttons.buy"/>
                        </Button>

                    </Col>

                </Form.Group>

            </Form>

        </div>

    );

};

export default BuyTicketsForm;