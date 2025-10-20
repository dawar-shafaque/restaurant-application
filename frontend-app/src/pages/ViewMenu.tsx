"use client";
 
import { useEffect, useState } from "react";
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import Image from "@/assets/headingImage.jpg";
import { Navbar } from "@/components/Navbar";
import { toast } from "react-toastify";
import { API } from "@/api/endpoints";
 
type Item = {
  price: string;
  imageUrl: string;
  previewImageUrl: string;
  name: string;
  weight: string;
  id: string;
  available: boolean;
  carbohydrates: string;
  vitamins: string;
  description: string;
  fats: string;
  proteins: string;
  calories: string;
};
 
export default function MenuPage() {
  const [items, setItems] = useState<Item[]>([]);
  const [sortBy, setSortBy] = useState("popularity,desc");
  const [filter, setFilter] = useState("Appetizers");
  const [selectedItem, setSelectedItem] = useState<Item | null>(null);
  useEffect(() => {
    setItems([]);
    (async function () {
      try {
        const t = await fetch(
          `${API.GET_DISHES}?dishType=${filter}&sort=${sortBy}`, {
            method:"GET",
            headers:{
                'Content-Type':"application/json",
            }
          }
        );
        const t1 = await t.json();
        setItems(t1);
      } catch (err: unknown) {
        if (err instanceof Error) {
          toast.error(err.message);
        } else {
          toast.error("An unexpected error occurred.");
        }
      }
    })();
  }, [filter, sortBy]);
  const dishInfo = async (id: string) => {
    const t = await fetch(
      `${API.GET_DISHES}/${id}`
    );
    const t1 = await t.json();
    setSelectedItem(t1);
  };
  return (
    <div>
      {/* Top Navbar */}
      <Navbar />
 
      {/* Page Content */}
      <div className="max-w-7xl mx-auto py-8 px-4">
        {/* Header Image */}
        <div className="relative h-56 w-full rounded-lg overflow-hidden mb-10">
          <img
            src={Image}
            alt="Menu Background"
            className="object-cover w-full h-full absolute inset-0"
          />
          <div className="absolute inset-0 bg-black/50 flex items-center px-6">
            <div>
              <h2 className="text-green-500 text-sm font-medium mb-1">
                Green & Tasty Restaurants
              </h2>
              <h1 className="text-white text-3xl font-bold">Menu</h1>
            </div>
          </div>
        </div>
 
        {/* Filters and Sort */}
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-8">
          <ToggleGroup
            type="single"
            defaultValue="appetizers"
            onValueChange={(val) => setFilter(val)}
            className="space-x-2 active:border-blue-500"
          >
            <ToggleGroupItem value="Appetizers">Appetizers</ToggleGroupItem>
            <ToggleGroupItem value="Main Courses">Main Courses</ToggleGroupItem>
            <ToggleGroupItem value="Desserts">Desserts</ToggleGroupItem>
          </ToggleGroup>
 
          <div className="flex items-center space-x-2">
            <span className="text-sm font-medium text-muted-foreground">
              Sort by:
            </span>
            <Select
              defaultValue="popularity,desc"
              onValueChange={(value) => setSortBy(value)}
            >
              <SelectTrigger className="w-[200px]">
                <SelectValue placeholder="Select a sort" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="popularity,desc">
                  Popularity Descending
                </SelectItem>
                <SelectItem value="popularity,asc">
                  Popularity Ascending
                </SelectItem>
                <SelectItem value="price,asc">Price Ascending</SelectItem>
                <SelectItem value="price,desc">Price Descending</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>
 
        {/* Cards Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
          {items.length > 0 &&
            items.map((item) => (
              <Card
                key={item.id}
                onClick={() => dishInfo(item.id)}
                className="transition-transform transform hover:-translate-y-1 hover:shadow-lg duration-300 hover:cursor-pointer"
              >
                <CardHeader className="p-0">
                  <div className="relative w-full h-36 rounded-t-md overflow-hidden flex items-center justify-center">
                    <img
                      src={item.previewImageUrl}
                      alt={item.name}
                      className={`w-32 h-32 transition-all rounded-full duration-300 object-cover ${
                        !item.available  ? "opacity-50 grayscale" : ""
                      }`}
                    />
                    {!item.available && (
                      <span className="absolute top-2 right-2 bg-red-500 text-white text-xs font-semibold px-2 py-0.5 rounded-full">
                        On Stop
                      </span>
                    )}
                  </div>
                </CardHeader>
 
                <CardContent className="p-3">
                  {/* Slightly less padding */}
                  <CardTitle className="text-sm font-semibold">
                    {item.name}
                  </CardTitle>
                  <p className="text-xs text-muted-foreground">{item.weight}</p>
                </CardContent>
                <CardFooter className="flex justify-between items-center px-3 pb-3">
                  <span className="font-semibold text-green-700 text-sm">
                    ${item.price}
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={!item.available }
                  >
                    {item.available ? "Add to Cart" : "Unavailable"}
                  </Button>
                </CardFooter>
              </Card>
            ))}
          {selectedItem && (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4 sm:px-0">
              <div className="relative bg-white rounded-2xl shadow-lg w-full max-w-lg sm:max-w-md p-6 sm:p-8">
                {/* Close Button */}
                <button
                  onClick={() => setSelectedItem(null)}
                  className="absolute top-2 right-2 text-gray-500 hover:text-black text-xl"
                  aria-label="Close modal"
                >
                  &times;
                </button>
 
                {/* Modal Content */}
                <div className="text-center">
                  <img
                    src={selectedItem.imageUrl}
                    alt={selectedItem.name}
                    className="w-28 h-28 sm:w-32 sm:h-32 object-cover rounded-full mx-auto mb-4"
                  />
 
                  <h2 className="text-xl sm:text-2xl font-bold mb-2">
                    {selectedItem.name}
                  </h2>
                  <p className="text-sm text-muted-foreground mb-4">
                    {selectedItem.description || "No description provided."}
                  </p>
 
                  <div className="text-sm text-left space-y-1">
                    <p>
                      <strong>Calories:</strong>{" "}
                      {selectedItem.calories || "N/A"}
                    </p>
                    <p>
                      <strong>Protein:</strong> {selectedItem.proteins || "N/A"}{" "}
                    </p>
                    <p>
                      <strong>Fats:</strong> {selectedItem.fats || "N/A"}{" "}
                    </p>
                    <p>
                      <strong>Carbohydrates:</strong>{" "}
                      {selectedItem.carbohydrates || "N/A"}{" "}
                    </p>
                    <p>
                      <strong>Vitamins:</strong>{" "}
                      {selectedItem.vitamins || "N/A"}
                    </p>
                  </div>
 
                  <div className="flex justify-between items-center mt-5 text-sm font-semibold">
                    <span>${selectedItem.price}</span>
                    <span>{selectedItem.weight}</span>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
 
 