import {useEffect, useState} from 'react';
import {useParams, Link} from 'react-router';
import {useSelector} from 'react-redux';
import {FormattedDate, FormattedMessage, FormattedNumber, FormattedTime} from 'react-intl';
import Card from 'react-bootstrap/Card';
import Alert from 'react-bootstrap/Alert';

import backend from '../../../backend';
import users from '../../users';
import {BackLink, Errors} from '../../common';
import {BuyTicketsForm} from '../../shopping';

const SessionDetails = () => {

    const {id} = useParams();

    const [session, setSession] = useState(null);
    const [backendErrors, setBackendErrors] = useState(null);

    const loggedIn = useSelector(users.selectors.isLoggedIn);
    const isViewer = useSelector(users.selectors.isViewer);

    const loadSession = async () => {
        if (!Number.isNaN(Number(id))) {
            const response = await backend.catalogService.getSessionDetails(id);

            if (response.ok) {
                setSession(response.payload);
                setBackendErrors(null);
            } else {
                setBackendErrors(response.payload);
            }
        }
    };

    useEffect(() => {

        loadSession();

    }, [id]);

    if (!session) {
        if (backendErrors) {
            return (
                <div>
                    <Errors errors={backendErrors}
                            onClose={() => setBackendErrors(null)}/>
                    <div className="mt-3">
                        <BackLink/>
                    </div>
                </div>
            )
        }
        return null;
    }

    const sessionDate = new Date(session.date);
    const currentDate = new Date();

    const showBuyTicketsForm =
        loggedIn &&
        isViewer &&
        sessionDate > currentDate &&
        session.freeSeats > 0;

    return (

        <Card>

            <Card.Body>

                <Card.Title>

                    <Link id="movieTitle"
                          to={`/catalog/movie-details/${session.movieId}`}>

                        {session.movieTitle}

                    </Link>

                </Card.Title>

                <Card.Text id="runtime">

                    <strong>
                        <FormattedMessage id="project.catalog.SessionDetails.fields.duration"/>:
                    </strong>{' '}

                    {session.movieDuration} min

                </Card.Text>

                <Card.Text id="price">

                    <strong>
                        <FormattedMessage id="project.catalog.SessionDetails.fields.price"/>:
                    </strong>{' '}

                    <FormattedNumber value={session.price}
                                     style="currency"
                                     currency="EUR"/>

                </Card.Text>

                <Card.Text id="date">

                    <strong>
                        <FormattedMessage id="project.catalog.SessionDetails.fields.date"/>:
                    </strong>{' '}

                    <FormattedDate value={sessionDate}/>

                </Card.Text>

                <Card.Text id="time">

                    <strong>
                        <FormattedMessage id="project.catalog.SessionDetails.fields.time"/>:
                    </strong>{' '}

                    <FormattedTime value={sessionDate}
                                   hour="2-digit"
                                   minute="2-digit"/>

                </Card.Text>

                <Card.Text id="roomName">

                    <strong>
                        <FormattedMessage id="project.catalog.SessionDetails.fields.room"/>:
                    </strong>{' '}

                    {session.roomName}

                </Card.Text>

                <Card.Text id="freeSeats">

                    <strong>
                        <FormattedMessage id="project.catalog.SessionDetails.fields.freeSeats"/>:
                    </strong>{' '}

                    {session.freeSeats}

                </Card.Text>

                {loggedIn &&
                    isViewer &&
                    session.freeSeats === 0 &&

                    <Alert variant="info" id="noFreeSeats">

                        <FormattedMessage id="project.catalog.SessionDetails.noFreeSeats"/>

                    </Alert>

                }

                {showBuyTicketsForm &&

                    <BuyTicketsForm sessionId={session.id}
                                    onPurchaseCompleted={loadSession}/>

                }

                <div className="mt-3">

                    <BackLink/>

                </div>

            </Card.Body>

        </Card>

    );

};

export default SessionDetails;