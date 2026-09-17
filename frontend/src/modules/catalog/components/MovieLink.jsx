import {Link} from 'react-router';

const MovieLink = ({id, name}) => {

    return (
        <Link to={`/catalog/movie-details/${id}`}>
            {name}
        </Link>
    );

}

export default MovieLink;