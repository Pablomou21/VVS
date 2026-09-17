import {useEffect, useState} from 'react';
import {useParams} from 'react-router';
import {FormattedMessage} from 'react-intl';
import Card from 'react-bootstrap/Card';
import backend from '../../../backend';
import {BackLink} from '../../common';

const MovieDetails = () => {

    const {id} = useParams();
    const [movie, setMovie] = useState(null);

    useEffect(() => {

        const loadMovie = async () => {
            if (!Number.isNaN(id)) {
                const response = await backend.catalogService.getMovieDetails(id);

                if (response.ok) {
                    setMovie(response.payload);
                }
            }
        };

        loadMovie();

    }, [id]);

    if (!movie) {
        return null;
    }

    return (
        <Card>
            <Card.Body>
                <Card.Title id="movieTitle">
                    {movie.title}
                </Card.Title>

                <Card.Text id="summary">
                    <strong>
                        <FormattedMessage id="project.catalog.MovieDetails.fields.summary"/>:
                    </strong>{' '}
                    {movie.summary}
                </Card.Text>

                <Card.Text id="duration">
                    <strong>
                        <FormattedMessage id="project.catalog.MovieDetails.fields.duration"/>:
                    </strong>{' '}
                    {movie.duration} min
                </Card.Text>

                <BackLink/>
            </Card.Body>
        </Card>
    );

};

export default MovieDetails;