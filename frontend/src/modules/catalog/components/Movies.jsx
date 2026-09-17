import {FormattedMessage} from 'react-intl';
import Table from 'react-bootstrap/Table';

import MovieLink from './MovieLink';
import SessionLink from './SessionLink'

const Movies = ({movies}) => (

    <Table striped hover>

        <thead>
        <tr>
            <th>
                <FormattedMessage id='project.catalog.Movies.fields.billboard'/>
            </th>
        </tr>
        </thead>

        <tbody id="movies-tbody">
        {movies.map(movie => (
            <tr key={movie.movieId}>
                <td>
                    <div id={`movie.${movie.movieId}`}>
                        <MovieLink id={movie.movieId} name={movie.movieTitle}/>
                    </div>
                    <div>
                        {movie.sessions.map(session => (
                            <span key={session.id} id={`session.${session.id}`}>
                                <SessionLink
                                    id={session.id}
                                    date={session.date}
                                />
                                &nbsp;&nbsp;
                            </span>
                        ))}
                    </div>
                </td>
            </tr>
        ))}
        </tbody>

    </Table>

);

export default Movies;
