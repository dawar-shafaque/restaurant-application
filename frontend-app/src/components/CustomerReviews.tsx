import { Review } from "@/types/FormData";
import { useEffect, useState } from "react";
import { IoIosArrowDown } from "react-icons/io";
import { API } from "@/api/endpoints";

const sortOptions = [
  {
    label: "Top rated first",
    value: "best",
    sortField: "rating",
    direction: "desc",
  },
  {
    label: "Low rated first",
    value: "worst",
    sortField: "rating",
    direction: "asc",
  },
  {
    label: "Newest first",
    value: "newest",
    sortField: "date",
    direction: "desc",
  },
  {
    label: "Oldest first",
    value: "oldest",
    sortField: "date",
    direction: "asc",
  },
];

const CustomerReviews: React.FC<{ id: string }> = ({ id }) => {
  const [activeTab, setActiveTab] = useState("service");
  const [selectedSort, setSelectedSort] = useState("best");
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [pageInfo, setPageInfo] = useState({
    number: 0,
    totalPages: 0,
    size: 3,
  });
  const [page, setPage] = useState(0);

  const currentSortOption = sortOptions.find(
    (opt) => opt.value === selectedSort
  );

  const fetchReviews = async () => {
    const type = activeTab === "service" ? "service" : "Cuisine";
    const sortField = currentSortOption?.value || "rating";
    // const direction = currentSortOption?.direction || "desc";

    try {
      const res = await fetch(
        `${API.REVIEW_API}/${id}/feedbacks?type=${type}&sortBy=${sortField}&page=${page}&size=6`
      );
      const { content, totalPages, number, size } = await res.json();
      setReviews(content);
      setPageInfo({ totalPages, number, size });
    } catch (error) {
      console.error("Failed to fetch reviews", error);
    }
  };

  useEffect(() => {
    fetchReviews();
  }, [activeTab, selectedSort, page]);

  // Handlers
  const handleTabChange = (tab: string) => {
    setActiveTab(tab);
    setPage(0);
  };

  const handleSortChange = (value: string) => {
    setSelectedSort(value);
    setDropdownOpen(false);
    setPage(0);
  };

  const handlePageChange = (newPage: number) => {
    if (newPage >= 0 && newPage < pageInfo.totalPages) {
      setPage(newPage);
    }
  };

  return (
    <div className="w-full mx-auto p-6">
      <h2 className="text-2xl font-semibold">Customer Reviews</h2>

      {/* Tabs */}
      <div className="flex space-x-6 border-b mt-4">
        <button
          className={`pb-2 font-medium ${
            activeTab === "service"
              ? "border-b-2 border-green-500 text-green-600"
              : "text-gray-500"
          }`}
          onClick={() => handleTabChange("service")}
        >
          Service
        </button>
        <button
          className={`pb-2 font-medium ${
            activeTab === "cuisine"
              ? "border-b-2 border-green-500 text-green-600"
              : "text-gray-500"
          }`}
          onClick={() => handleTabChange("cuisine")}
        >
          Cuisine experience
        </button>
      </div>

      {/* Sort dropdown */}
      <div className="flex justify-end mt-4">
        <div className="relative">
          <button
            className="bg-green-100 text-green-700 px-4 py-2 rounded-md flex items-center space-x-2"
            onClick={() => setDropdownOpen(!dropdownOpen)}
          >
            <span>{currentSortOption?.label}</span>
            <IoIosArrowDown />
          </button>

          {dropdownOpen && (
            <div className="absolute right-0 mt-2 bg-white shadow-lg rounded-md w-40 z-10">
              {sortOptions.map((option) => (
                <button
                  key={option.value}
                  className="block w-full text-left px-4 py-2 text-sm hover:bg-green-100"
                  onClick={() => handleSortChange(option.value)}
                >
                  {option.label}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Reviews */}
      <div className="p-6">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {reviews.map((review) => (
            <div key={review.id} className="bg-white shadow-md rounded-lg p-4">
              <div className="flex items-center space-x-4">
                <img
                  src={review.userAvatarUrl}
                  alt={review.userName}
                  className="w-12 h-12 rounded-full object-cover"
                />
                <div>
                  <h4 className="font-medium">{review.userName}</h4>
                  <p className="text-gray-500 text-sm">
                    Date: {review.date.split("T")[0]}
                  </p>
                  <p className="font-semibold">Rating: {review.rate}</p>
                </div>
              </div>
              <p className="text-gray-600 mt-2">{review.comment}</p>
            </div>
          ))}
        </div>

        {/* Pagination */}
        <div className="flex justify-center mt-6 space-x-2">
          <button
            onClick={() => handlePageChange(page - 1)}
            disabled={page === 0}
            className="px-4 py-2 bg-gray-200 rounded disabled:opacity-50"
          >
            Prev
          </button>

          {Array.from({ length: pageInfo.totalPages }, (_, i) => (
            <button
              key={i}
              onClick={() => handlePageChange(i)}
              className={`px-4 py-2 rounded ${
                i === page ? "bg-blue-500 text-white" : "bg-gray-100"
              }`}
            >
              {i + 1}
            </button>
          ))}

          <button
            onClick={() => handlePageChange(page + 1)}
            disabled={page + 1 === pageInfo.totalPages}
            className="px-4 py-2 bg-gray-200 rounded disabled:opacity-50"
          >
            Next
          </button>
        </div>
      </div>
    </div>
  );
};

export default CustomerReviews;
