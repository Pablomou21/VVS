import {Link} from 'react-router';
import {FormattedTime} from 'react-intl';

const SessionLink = ({id, date}) => {

    return (
        <Link to={`/catalog/session-details/${id}`}>
            <FormattedTime
                value={new Date(date)}
                hour="2-digit"
                minute="2-digit"
            />
        </Link>
    );

}

export default SessionLink;