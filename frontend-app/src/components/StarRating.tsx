import { FaStar } from "react-icons/fa";
 
interface StarRatingProps {
  rating: number;
  setRating: (rating: number) => void;
}
 
const StarRating: React.FC<StarRatingProps> = ({ rating, setRating }) => {
  const handleClick = (index: number) => {
    setRating(index);
  };
 
  return (
    <div className="flex items-center space-x-2">
      {[1, 2, 3, 4, 5].map((star) => (
        <FaStar key={star}
          size={24}
          className={`cursor-pointer ${star <= rating ? 'text-yellow-500' : 'text-gray-300'}`}
          onClick={() => handleClick(star)}
          data-testid="ratingButton"
          role="button"
          aria-label={`Rate ${star} stars`}
        />
      ))}
    </div>
  );
};
 
export default StarRating;