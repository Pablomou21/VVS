import Alert from 'react-bootstrap/Alert';

const Success = ({message, onClose, id}) => message && (
    <Alert id={id} variant="success" onClose={() => onClose()} dismissible>
        {message}
    </Alert>
);

export default Success;
