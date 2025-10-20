import CustomerReviews from '@/components/CustomerReviews';
import LocationHeroSection from '@/components/LocationHeroSection';
import { Navbar } from '@/components/Navbar';
import { SpecialityDishes } from '@/components/SpecialityDishes';
import { useParams } from 'react-router-dom';

const LocationDetails = () => {
  const { id } = useParams();
  return (
    <div>
      <Navbar />
      <LocationHeroSection />
      {
        id ? (
          <SpecialityDishes id={id}/>
        ):(<p>Error: Location ID is missing.</p>)
      }
      {id ? (
        <CustomerReviews id={id} />
      ) : (
        <p>Error: Location ID is missing.</p>
      )}
    </div>
  )
}

export default LocationDetails
