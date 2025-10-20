import { HeroSection } from "@/components/HeroSection";
import { Navbar } from "@/components/Navbar";
import { PopularDishes } from "@/components/PopularDishes";
import { lazy, Suspense } from "react";
const Locations = lazy(() => import("@/components/Locations"));
const Landing = () => {
  return (
    <div>
      <Navbar />
      <HeroSection />
      <PopularDishes />
      <Suspense
        fallback={
          <div className="flex justify-center items-center" data-testid="LoadingContainer">Loading...</div>
        }
      >
        <Locations />
      </Suspense>
    </div>
  );
};

export default Landing;
